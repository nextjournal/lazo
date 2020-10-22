(ns lazo.core
  (:gen-class)
  (:require [aero.core :as aero]
            [clojure.core.async :as async]
            [clojure.tools.logging :as log]
            [lazo.events :as events]
            [mount.core :as mount]
            [muuntaja.core :as m]
            [reitit.ring :as ring]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.params :as params]
            [lazo.git :as git]))

(def event-queue (async/chan 10))

(def config
  (aero/read-config "config.edn"))

(mount/defstate repos
  :start (git/initialize-repos! config))

(mount/defstate event-processor
  :start (async/go-loop []
           (let [event (async/<! event-queue)]
             (try
               (log/info :event (:event/type event))
               (events/handle-event! {:config config
                                      :event  event})
               (catch Exception _e))
             (recur))))


(defn handle-post [req]
  (if-let [type (get-in req [:headers "x-github-event"])]
    (let [event (assoc (:body-params req)
                  :event/type (keyword type))]
      (async/>!! event-queue event)
      {:status 200 :body "OK"})
    {:status 400 :body "Not a valid request"}))

(def app
  (ring/ring-handler
    (ring/router
      ["/" {:post {:handler handle-post}
            :get  {:handler (fn [_] {:status 200 :body "This service is API only"})}}]
      {:data {:muuntaja   m/instance
              :middleware [params/wrap-params
                           muuntaja/format-middleware]}})
    (ring/create-default-handler)))


(mount/defstate server
  :start (jetty/run-jetty #'app {:port 8890, :join? false})
  :stop (.stop server))


(defn go []
  (mount/start))

(defn -main [& _args]
  (go))
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
            [lazo.git :as git]
            [me.raynes.fs :as fs]))

(def event-queue (async/chan 10))

(defn config-file []
  (or (System/getenv "LAZO_CONFIG_FILE")
      "config.edn"))

(mount/defstate config
  :start (aero/read-config (config-file)))

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
  (if (fs/exists? (config-file))
     (mount/start)
     (do (log/error "config.edn not found.")
         (System/exit 1))))

(defn -main [& _args]
  (go))
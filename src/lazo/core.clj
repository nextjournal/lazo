(ns lazo.core
  (:require [ring.adapter.jetty :as jetty]
            [clojure.tools.logging :as log]
            [reitit.ring :as ring]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [muuntaja.core :as m]
            [mount.core :as mount]
            [lazo.events :as events]
            [ring.middleware.params :as params]))

(def config
  {:user      "Heliosmaster"
   :token     "TOKEN"
   :local-dir ".lazo-repos"
   :email     "foo@example.com"
   :repos     [{:organization       "test-org-integration"
                :main-repo          "main-repo"
                :main-branch        "master"
                :main-module-folder "my-module"
                :module-repo        "module-repo"
                :module-branch      "master"}
               {:organization       "test-org-integration"
                :main-repo          "main-repo"
                :main-branch        "master"
                :main-module-folder "my-module2"
                :module-repo        "module2-repo"
                :module-branch      "master"}]})


(defn handle-post [req]
  (let [event (assoc (:body-params req)
                :event/type (keyword (get-in req [:headers "x-github-event"])))]
    (events/handle-event! {:config config
                           :event  event}))
  {:status 200 :body "OK"})

(def app
  (ring/ring-handler
    (ring/router
      ["/" {:post {:handler handle-post}
            :get  {:handler
                   (fn [_] (log/info :foo "bar")
                     {:status 200 :body "This service is API only"})}}]
      {:data {:muuntaja   m/instance
              :middleware [params/wrap-params
                           muuntaja/format-middleware]}})
    (ring/create-default-handler)))


(mount/defstate server
  :start (jetty/run-jetty #'app {:port 8890, :join? false})
  :stop (.stop server))

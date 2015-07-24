(ns user-http-api.server
  (:require [user-http-api.service :as service]
            [io.pedestal.http :as http]
            [turbovote.resource-config :refer [config]]
            [immutant.util :as immutant]
            [user-http-api.channels :as channels]
            [user-http-api.queue :as queue]))

(defn shutdown [rabbit-resources]
  (channels/close-all!)
  (queue/close-all! rabbit-resources))

(defn start-http-server [& [options]]
  (-> (service/service)
      (merge options)
      http/create-server
      http/start))

(defn -main [& args]
  (let [rabbit-resources (queue/initialize)]
    (start-http-server)
    (immutant/at-exit (partial shutdown rabbit-resources))))

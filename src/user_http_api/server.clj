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

(defn -main [& args]
  (let [rabbit-resources (queue/initialize)]
    (-> (service/service)
        http/create-server
        http/start)
    (immutant/at-exit (partial shutdown rabbit-resources))))

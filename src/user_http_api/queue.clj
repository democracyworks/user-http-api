(ns user-http-api.queue
  (:require [clojure.tools.logging :as log]
            [langohr.core :as rmq]
            [langohr.channel :as lch]
            [langohr.exchange :as le]
            [langohr.queue :as lq]
            [kehaar.rabbitmq]
            [kehaar.wire-up :as wire-up]
            [user-http-api.channels :as channels]
            [turbovote.resource-config :refer [config]]))

(defn initialize []
  (let [max-retries 5
        rabbit-config (config [:rabbitmq :connection])
        connection (kehaar.rabbitmq/connect-with-retries rabbit-config max-retries)]
    {:connections [connection]
     :channels [(wire-up/external-service
                 connection
                 ""
                 "user-works.user.create"
                 (config [:rabbitmq :queues "user-works.user.create"])
                 (config [:timeouts :user-create])
                 channels/create-users)
                (wire-up/external-service
                 connection
                 ""
                 "user-works.user.read"
                 (config [:rabbitmq :queues "user-works.user.read"])
                 (config [:timeouts :user-read])
                 channels/read-users)
                (wire-up/external-service
                 connection
                 ""
                 "user-works.user.update"
                 (config [:rabbitmq :queues "user-works.user.update"])
                 (config [:timeouts :user-update])
                 channels/update-users)
                (wire-up/external-service
                 connection
                 ""
                 "user-works.user.delete"
                 (config [:rabbitmq :queues "user-works.user.delete"])
                 (config [:timeouts :user-delete])
                 channels/delete-users)]}))

(defn close-resources! [resources]
  (doseq [resource resources]
    (when-not (rmq/closed? resource) (rmq/close resource))))

(defn close-all! [{:keys [connections channels]}]
  (close-resources! channels)
  (close-resources! connections))

(ns user-http-api.queue
  (:require [clojure.tools.logging :as log]
            [langohr.core :as rmq]
            [langohr.channel :as lch]
            [langohr.exchange :as le]
            [langohr.queue :as lq]
            [kehaar.core :as k]
            [user-http-api.channels :as channels]
            [user-http-api.handlers :as handlers]
            [turbovote.resource-config :refer [config]]))

(defn uber-wire-up!
  [rabbit-ch async-ch queue-name]
  (lq/declare rabbit-ch
              queue-name
              (config :rabbitmq :queues queue-name))
  (k/wire-up-service rabbit-ch queue-name async-ch))

(defn initialize []
  (let [connection (atom nil)
        max-retries 5]
    (loop [attempt 1]
      (try
        (reset! connection
                (rmq/connect (or (config :rabbitmq :connection)
                                 {})))
        (log/info "RabbitMQ connected.")
        (catch Throwable t
          (log/warn "RabbitMQ not available:" (.getMessage t)
                    "attempt:" attempt)))
      (when (nil? @connection)
        (if (< attempt max-retries)
          (do (Thread/sleep (* attempt 1000))
              (recur (inc attempt)))
          (do (log/error "Connecting to RabbitMQ failed. Bailing.")
              (throw (ex-info "Connecting to RabbitMQ failed"
                              {:attempts attempt}))))))
    (let [ok-ch (lch/open @connection)
          user-create-ch (lch/open @connection)]
      (lq/declare ok-ch
                  "user-http-api.ok"
                  (config :rabbitmq :queues "user-http-api.ok"))
      (k/responder ok-ch "user-http-api.ok" handlers/ok)
      (uber-wire-up! user-create-ch
                     channels/create-users
                     "user-works.user.create")
      {:connections #{@connection}
       :channels #{ok-ch user-create-ch}})))

(defn close-resources! [resources]
  (doseq [resource resources]
    (when-not (rmq/closed? resource) (rmq/close resource))))

(defn close-all! [{:keys [connections channels]}]
  (close-resources! channels)
  (close-resources! connections))

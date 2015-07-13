(ns user-http-api.core
    (:require [user-http-api.channels :as channels]
              [user-http-api.queue :as queue]
              [turbovote.resource-config :refer [config]]
              [clojure.tools.logging :as log]
              [immutant.util :as immutant]))

(defn -main [& args]
  (let [rabbit-resources (queue/initialize)]
    (immutant/at-exit (fn []
                        (queue/close-all! rabbit-resources)
                        (channels/close-all!)))))

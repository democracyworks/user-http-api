(ns user-http-api.channels
  (:require [clojure.core.async :as async]))

(defonce create-users (async/chan))
(defonce read-users (async/chan))
(defonce update-users (async/chan))
(defonce delete-users (async/chan))

(defn start-event-handler!
  "Start a new thread listening for messages on `channel` and passing
  them to `handler`. Will loop over all messages, logging errors. When
  `channel` is closed, stop looping. Or the loop can be stopped by
  calling the `:stop!` function returned."
  [channel handler]
  (let [stop-chan (async/chan)
        signal-chan (async/thread
                      (loop []
                        (async/alt!!
                          stop-chan :stopped
                          channel ([event]
                                   (if (nil? event)
                                     :closed
                                     (do
                                      (try
                                        (handler event)
                                        (catch Throwable t
                                          (println t "Error handling event."
                                                   (pr-str event))))
                                      (recur)))))))]
    {:stop! (fn [] (async/put! stop-chan :stop))
     :notes signal-chan}))

(defn close-all! []
  (doseq [c [create-users read-users update-users delete-users]]
    (async/close! c)))

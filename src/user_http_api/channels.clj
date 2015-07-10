(ns user-http-api.channels
  (:require [clojure.core.async :as async]))

;;; TODO: Create channels and add them to the list of channels to close.
(defn close-all! []
  (doseq [c []]
    (async/close! c)))

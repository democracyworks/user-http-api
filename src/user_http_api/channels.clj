(ns user-http-api.channels
  (:require [clojure.core.async :as async]))

(defonce create-users (async/chan))
(defonce read-users (async/chan))
(defonce update-users (async/chan))
(defonce delete-users (async/chan))

(defn close-all! []
  (doseq [c [create-users read-users update-users delete-users]]
    (async/close! c)))

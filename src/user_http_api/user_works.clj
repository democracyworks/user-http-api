(ns user-http-api.user-works
  (:require [kehaar.core :as k]
            [user-http-api.channels :as channels]))

(def create-user (k/ch->response-fn channels/create-users))
(def read-user (k/ch->response-fn channels/read-users))
(def update-user (k/ch->response-fn channels/update-users))
(def delete-user (k/ch->response-fn channels/delete-users))

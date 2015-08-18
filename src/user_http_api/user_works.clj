(ns user-http-api.user-works
  (:require [kehaar.wire-up :as wire-up]
            [user-http-api.channels :as channels]))

(def create-user (wire-up/async->fn channels/create-users))
(def read-user (wire-up/async->fn channels/read-users))
(def update-user (wire-up/async->fn channels/update-users))
(def delete-user (wire-up/async->fn channels/delete-users))

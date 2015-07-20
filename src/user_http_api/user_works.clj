(ns user-http-api.user-works
  (:require [kehaar.core :as kehaar]
            [user-http-api.channels :as channels]))

(def create-election (kehaar/ch->response-fn channels/create-users))

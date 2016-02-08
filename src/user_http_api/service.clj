(ns user-http-api.service
  (:require [io.pedestal.http :as bootstrap]
            [io.pedestal.http.route :as route]
            [io.pedestal.http.route.definition :refer [defroutes]]
            [io.pedestal.interceptor :refer [interceptor]]
            [ring.util.response :as ring-resp]
            [turbovote.resource-config :refer [config]]
            [pedestal-toolbox.cors :as cors]
            [pedestal-toolbox.params :refer :all]
            [pedestal-toolbox.content-negotiation :refer :all]
            [kehaar.core :as k]
            [user-http-api.channels :as channels]
            [bifrost.core :as bifrost]))

(def ping
  (interceptor
   {:enter
    (fn [ctx]
      (assoc ctx :response (ring-resp/response "OK")))}))

(def api-translator
  (interceptor
   {:enter
    (fn [ctx]
      (let [id-key-path [:request :path-params :id]]
        (if-let [user-id (get-in ctx id-key-path)]
          (assoc-in ctx id-key-path (java.util.UUID/fromString user-id))
          ctx)))
    :leave
    (fn [ctx]
      (if-let [user (get-in ctx [:response :body :user])]
        (assoc-in ctx [:response :body] user)
        ctx))}))

(defroutes routes
  [[["/"
     {:post [:post-user (bifrost/interceptor channels/create-users)]}
     ^:interceptors [(body-params)
                     query-param-accept
                     (negotiate-response-content-type ["application/edn"
                                                       "application/transit+json"
                                                       "application/transit+msgpack"
                                                       "application/json"
                                                       "text/plain"])
                     api-translator]
     ["/ping" {:get [:ping ping]}]
     ["/:id" {:get [:get-user (bifrost/interceptor channels/read-users)]
              :put [:put-user (bifrost/interceptor channels/update-users)]
              :patch [:patch-user (bifrost/interceptor channels/update-users)]
              :delete [:delete-user (bifrost/interceptor channels/delete-users)]}]]]])

(defn service []
  {::env :prod
   ::bootstrap/router :linear-search ; we need this router to support both /:id & /ping
   ::bootstrap/routes routes
   ::bootstrap/resource-path "/public"
   ::bootstrap/allowed-origins (cors/domain-matcher-fn
                                (map re-pattern
                                     (config [:server :allowed-origins])))
   ::bootstrap/host (config [:server :hostname])
   ::bootstrap/type :immutant
   ::bootstrap/port (config [:server :port])})

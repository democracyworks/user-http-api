(ns user-http-api.service
  (:require [io.pedestal.http :as bootstrap]
            [io.pedestal.http.route :as route]
            [io.pedestal.http.route.definition :refer [defroutes]]
            [io.pedestal.interceptor :refer [interceptor]]
            [ring.util.response :as ring-resp]
            [turbovote.resource-config :refer [config]]
            [pedestal-toolbox.params :refer :all]
            [pedestal-toolbox.content-negotiation :refer :all]
            [kehaar.core :as k]
            [clojure.core.async :refer [chan go alt! timeout]]
            [user-http-api.channels :as channels]
            [bifrost.core]
            [clojure.tools.logging :as log]))

(def ping
  (interceptor
   {:enter
    (fn [ctx]
      (assoc ctx :response (ring-resp/response "OK")))}))

(def response-timeout 20000)

;; Are we using this anywhere?
(def query-param-accept
  "A before interceptor that fakes an Accept header so that later
  interceptors can handle the Accept header normally. This is used
  because it's literally impossible to make a clickable link in a
  browser that sets the Accept header in the normal way.

  To use, add an accept=application/csv to the URL query string."
  (interceptor
   {:enter
    (fn [ctx]
      (if-let [accept-type (get-in ctx [:request :params :accept])]
        (assoc-in ctx [:request :headers "accept"] accept-type)
        ctx))}))

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

(def log-request
  (interceptor
   {:enter
    (fn [ctx]
      (log/info "Request:" (pr-str (:request ctx)))
      ctx)}))

(defroutes routes
  [[["/"
     {:post [:post-user channels/create-users]}
     ^:interceptors [(body-params)
                     query-param-accept
                     (negotiate-response-content-type ["application/edn"
                                                       "application/transit+json"
                                                       "application/transit+msgpack"
                                                       "application/json"
                                                       "text/plain"])
                     api-translator
                     log-request]
     ["/ping" {:get [:ping ping]}]
     ["/:id" {:get [:get-user channels/read-users]
              :put [:put-user channels/update-users]
              :patch [:patch-user channels/update-users]
              :delete [:delete-user channels/delete-users]}]]]])

(defn service []
  {::env :prod
   ::bootstrap/router :linear-search ; we need this router to support both /:id & /ping
   ::bootstrap/routes routes
   ::bootstrap/resource-path "/public"
   ::bootstrap/allowed-origins (if (= :all (config [:server :allowed-origins]))
                                 (constantly true)
                                 (config [:server :allowed-origins]))
   ::bootstrap/host (config [:server :hostname])
   ::bootstrap/type :immutant
   ::bootstrap/port (config [:server :port])})

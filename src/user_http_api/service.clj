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
            [clojure.core.async :refer [chan go <!]]
            [user-http-api.user-works :as uw]))

(def ping
  (interceptor
   {:enter
    (fn [ctx]
      (assoc ctx :response (ring-resp/response "OK")))}))

(defn rabbit-result->http-status
  [rabbit-result]
  (case (:status rabbit-result)
    :error 500)) ; TODO: Flesh this out once user-works gives us more error info

(def create-user
  (interceptor
   {:enter
    (fn [ctx]
      (let [user-data (get-in ctx [:request :body-params])]
        (go
          (let [result (<! (uw/create-user user-data))]
            (if (= (:status result) :ok)
              (let [user (:user result)]
                (assoc ctx :response
                       (ring-resp/created (str "/users/" (:id user)) user)))
              (let [http-status (rabbit-result->http-status result)]
                (assoc ctx :response
                       (-> result
                           (ring-resp/response)
                           (ring-resp/status http-status)))))))))}))


(def read-user
  (interceptor
   {:enter
    (fn [ctx]
      (let [user-id (-> ctx
                        (get-in [:request :path-params :id])
                        java.util.UUID/fromString)]
        (go
          (let [result (<! (uw/read-user {:id user-id}))]
            (if (= (:status result) :ok)
              (let [user (:user result)]
                (assoc ctx :response
                       (ring-resp/response user)))
              (let [http-status (rabbit-result->http-status result)]
                (assoc ctx :response
                       (-> result
                           (ring-resp/response)
                           (ring-resp/status http-status)))))))))}))

(def update-user
  (interceptor
   {:enter
    (fn [ctx]
      (let [user-id (-> ctx
                        (get-in [:request :path-params :id])
                        java.util.UUID/fromString)
            user-data (get-in ctx [:request :body-params])]
        (go
          (let [result (<! (uw/update-user (merge user-data {:id user-id})))]
            (if (= (:status result) :ok)
              (let [user (:user result)]
                (assoc ctx :response
                       (ring-resp/response user)))
              (let [http-status (rabbit-result->http-status result)]
                (assoc ctx :response
                       (-> result
                           (ring-resp/response)
                           (ring-resp/status http-status)))))))))}))

(def delete-user
  (interceptor
   {:enter
    (fn [ctx]
      (let [user-id (-> ctx
                        (get-in [:request :path-params :id])
                        java.util.UUID/fromString)]
        (go
          (let [result (<! (uw/delete-user {:id user-id}))]
            (if (= (:status result) :ok)
              (let [user (:user result)]
                (assoc ctx :response
                       (ring-resp/response user)))
              (let [http-status (rabbit-result->http-status result)]
                (assoc ctx :response
                       (-> result
                           (ring-resp/response)
                           (ring-resp/status http-status)))))))))}))

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

(defroutes routes
  [[["/"
     ^:interceptors [(body-params)
                     query-param-accept
                     (negotiate-response-content-type ["application/edn"
                                                       "application/transit+json"
                                                       "application/transit+msgpack"
                                                       "application/json"])]
     ["/ping" {:get [:ping ping]}]
     ["/users" {:post [:post-user create-user]}]
     ["/users/:id" {:get [:get-user read-user]
                    :put [:put-user update-user]
                    :patch [:patch-user update-user]
                    :delete [:delete-user delete-user]}]]]])

(defn service []
  {::env :prod
   ::bootstrap/routes routes
   ::bootstrap/resource-path "/public"
   ::bootstrap/host (config [:server :hostname])
   ::bootstrap/type :immutant
   ::bootstrap/port (config [:server :port])})
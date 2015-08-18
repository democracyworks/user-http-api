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
            [user-http-api.user-works :as uw]))

(def ping
  (interceptor
   {:enter
    (fn [ctx]
      (assoc ctx :response (ring-resp/response "OK")))}))

(defn rabbit-result->http-status
  [rabbit-result]
  (case (:status rabbit-result)
    :error 500
    :not-found 404
    500)) ; TODO: Flesh this out once user-works gives us more error info

(def response-timeout 20000)

(def create-user
  (interceptor
   {:enter
    (fn [ctx]
      (let [user-data (get-in ctx [:request :body-params])
            result-chan (uw/create-user user-data)]
        (go
          (let [result (alt! (timeout response-timeout) {:status :error
                                                         :error {:type :timeout}}
                             result-chan ([v] v))]
            (if (= (:status result) :ok)
              (let [user (:user result)]
                (assoc ctx :response
                       (ring-resp/created (str "/" (:id user)) user)))
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
                        java.util.UUID/fromString)
            result-chan (uw/read-user {:id user-id})]
        (go
          (let [result (alt! (timeout response-timeout) {:status :error
                                                         :error {:type :timeout}}
                             result-chan ([v] v))]
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
            user-data (get-in ctx [:request :body-params])
            result-chan (uw/update-user (merge user-data {:id user-id}))]
        (go
          (let [result (alt! (timeout response-timeout) {:status :error
                                                         :error {:type :timeout}}
                             result-chan ([v] v))]
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
                        java.util.UUID/fromString)
            result-chan (uw/delete-user {:id user-id})]
        (go
          (let [result (alt! (timeout response-timeout) {:status :error
                                                         :error {:type :timeout}}
                             result-chan ([v] v))]
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
     {:post [:post-user create-user]}
     ^:interceptors [(body-params)
                     query-param-accept
                     (negotiate-response-content-type ["application/edn"
                                                       "application/transit+json"
                                                       "application/transit+msgpack"
                                                       "application/json"
                                                       "text/plain"])]
     ["/ping" {:get [:ping ping]}]
     ["/:id" {:get [:get-user read-user]
              :put [:put-user update-user]
              :patch [:patch-user update-user]
              :delete [:delete-user delete-user]}]]]])

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

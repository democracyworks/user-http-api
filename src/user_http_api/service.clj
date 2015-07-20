(ns user-http-api.service
  (:require [io.pedestal.http :as bootstrap]
            [io.pedestal.http.route :as route]
            [io.pedestal.http.route.definition :refer [expand-routes]]
            [io.pedestal.interceptor.helpers :refer [handler]]
            [io.pedestal.interceptor :refer [interceptor]]
            [ring.util.response :as ring-resp]
            [turbovote.resource-config :refer [config]]
            [pedestal-toolbox.params :refer :all]
            [pedestal-toolbox.content-negotiation :refer :all]))

(def ping
  (handler
   (fn [request]
     (ring-resp/response "OK"))))

(def create-user
  (handler
   (fn [{:keys [first-name last-name email phone address
                registered-street registered-city registered-state
                registered-zip] :as request}]
     (let [user {:first-name first-name
                 :last-name last-name
                 :phone phone
                 :address address
                 :registered
                 {:street registered-street
                  :city registered-city
                  :state registered-state
                  :zip registered-zip}}]
       (ring-resp/response "Created user" user)))))

(def global-interceptors
  [(body-params)])

(def query-param-content-type
  "An enter interceptor that fakes an Accept header so that later
  interceptors can handle the Accept header normally. This is used
  because it's literally impossible to make a clickable link in a
  browser that sets the Accept header in the normal way.

  To use, add an content-type=application/csv to the URL query string."
  (interceptor
   {:enter
    (fn [ctx]
      (if-let [content-type (get-in ctx [:request :params :content-type])]
        (assoc-in ctx [:request :headers "accept"] content-type)
        ctx))}))

(def api-routes
  `[[["/"
      ^:interceptors [~@global-interceptors
                      query-param-content-type]
      ["/ping" {:get [:ping ping]}]
      ["/users" {:post [:users create-user]}]]]])

(def routes
  (mapcat expand-routes [api-routes]))

(defn service []
  (println "Starting Wildfly on port" (config :server :port))
  {::env :prod
   ::bootstrap/routes routes
   ::bootstrap/resource-path "/public"
   ::bootstrap/host (config :server :hostname)
   ::bootstrap/type :immutant
   ::bootstrap/port (config :server :port)})

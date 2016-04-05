(ns user-http-api.service-test
  (:require [user-http-api.server :as server]
            [user-http-api.channels :as channels]
            [clojure.test :refer :all]
            [clj-http.client :as http]
            [clojure.core.async :refer [go <! >! close!]]
            [clojure.tools.logging :as log]
            [cognitect.transit :as transit]
            [clojure.string :as str])
  (:import [java.io ByteArrayOutputStream ByteArrayInputStream]))

(def test-server-port 56080)

(defn start-test-server [run-tests]
  (server/start-http-server {:io.pedestal.http/port test-server-port})
  (run-tests))

(use-fixtures :once start-test-server)

(def root-url (str "http://localhost:" test-server-port))

(defn dummy-response
  ([ch data] (dummy-response ch data :ok))
  ([ch data status]
   (go
     (let [[response-ch message] (<! ch)]
       (log/debug :in :dummy-response "Received:" (pr-str message))
       (>! response-ch (merge data {:status status}))
       (close! response-ch)))))

(deftest create-user-test
  (testing "valid EDN POST to / puts create message on create-users channel"
    (let [user-data {:first-name "Troy"
                     :last-name "Tulowitski"
                     :email "troy@rockies.mlb.com"}]
      (dummy-response channels/create-users
                      {:user (merge {:id (java.util.UUID/randomUUID)} user-data)}
                      :created)
      (let [response (http/post (str root-url "/")
                                {:headers {"Content-Type" "application/edn"}
                                 :body (pr-str user-data)})
            create-data (clojure.edn/read-string (:body response))]
        (is (= 201 (:status response)))
        (is (= "Tulowitski" (:last-name create-data))))))

  (testing "valid Transit+JSON POST to / puts create message on create-users channel"
    (let [user-data {:first-name "Troy"
                     :last-name "Tulowitski"
                     :email "troy@rockies.mlb.com"}]
      (dummy-response channels/create-users
                      {:user (merge {:id (java.util.UUID/randomUUID)} user-data)}
                      :created)
      (let [transit-out (ByteArrayOutputStream.)
            transit-writer (transit/writer transit-out :json)
            post-body (do (transit/write transit-writer user-data)
                          (.toString transit-out "UTF-8"))
            response (http/post (str root-url "/")
                                {:headers {"Content-Type" "application/transit+json"
                                           "Accept" "application/transit+json"}
                                 :body post-body})
            transit-in (ByteArrayInputStream. (-> response
                                                  :body
                                                  (.getBytes "UTF-8")))
            transit-reader (transit/reader transit-in :json)
            create-data (transit/read transit-reader)]
        (is (= 201 (:status response)))
        (is (= "Tulowitski" (:last-name create-data))))))

  (testing "invalid EDN POST to / returns error"
    (let [user-data {:foo "bar"}]
      (dummy-response channels/create-users {:error {:type :server}
                                             :message "Fake error"} :error)
      (let [response (http/post (str root-url "/")
                                {:headers {"Content-Type" "application/edn"}
                                 :body (pr-str user-data)
                                 :throw-exceptions false})
            create-data (clojure.edn/read-string (:body response))]
        (is (= 500 (:status response)))
        (is (= :server (get-in create-data [:error :type])))
        (is (= "Fake error" (:message create-data)))))))

(deftest read-user-test
  (testing "GET to /:id of existing user puts read message on read-users channel"
    (let [user-id (java.util.UUID/randomUUID)
          user-data {:id user-id
                     :first-name "Troy"
                     :last-name "Tulowitski"
                     :email "troy@rockies.mlb.com"}]
      (dummy-response channels/read-users {:user user-data})
      (let [response (http/get (str/join "/" [root-url user-id])
                               {:accept :edn})
            read-data (clojure.edn/read-string (:body response))]
        (is (= 200 (:status response)))
        (is (= "Tulowitski" (:last-name read-data))))))

  (testing "GET existing /users/:id with Transit+JSON response"
    (let [user-id (java.util.UUID/randomUUID)
          user-data {:id user-id
                     :first-name "Troy"
                     :last-name "Tulowitski"
                     :email "troy@rockies.mlb.com"}]
      (dummy-response channels/read-users {:user user-data})
      (let [response (http/get (str/join "/" [root-url user-id])
                               {:accept :transit+json})
            transit-in (ByteArrayInputStream. (-> response
                                                  :body
                                                  (.getBytes "UTF-8")))
            transit-reader (transit/reader transit-in :json)
            read-data (transit/read transit-reader)]
        (is (= 200 (:status response)))
        (is (= "Tulowitski" (:last-name read-data))))))

  (testing "GET non-existent /users/:id returns error"
    (dummy-response channels/read-users {:error {:type :not-found}
                                         :message "No such user"} :error)
    (let [response (http/get (str/join "/" [root-url (java.util.UUID/randomUUID)])
                              {:accept :edn, :throw-exceptions false})
          read-data (clojure.edn/read-string (:body response))]
      (is (= 404 (:status response)))
      (is (= :not-found (get-in read-data [:error :type])))
      (is (= "No such user" (:message read-data))))))

(deftest update-user-test
  (testing "PUT to /users/:id of existing user puts update message on update-users channel"
    (let [user-id (java.util.UUID/randomUUID)
          user-data {:first-name "Troy"
                     :last-name "Hill"
                     :email "escpawed@example.com"}]
      (dummy-response channels/update-users {:user user-data})
      (let [response (http/put (str/join "/" [root-url user-id])
                               {:body (pr-str user-data)
                                :content-type :edn
                                :accept :edn})
            update-data (clojure.edn/read-string (:body response))]
        (is (= 200 (:status response)))
        (is (= "Hill" (:last-name update-data))))))

  (testing "PATCH existing /users/:id with Transit+JSON body & response"
    (let [user-id (java.util.UUID/randomUUID)
          user-data {:first-name "Troy"
                     :last-name "Tulowitski"
                     :email "troy@rockies.mlb.com"}]
      (dummy-response channels/update-users {:user user-data})
      (let [transit-out (ByteArrayOutputStream.)
            transit-writer (transit/writer transit-out :json)
            post-body (do (transit/write transit-writer user-data)
                          (.toString transit-out "UTF-8"))
            response (http/patch (str/join "/" [root-url user-id])
                                 {:body post-body
                                  :content-type :transit+json
                                  :accept :transit+json})
            transit-in (ByteArrayInputStream. (-> response
                                                  :body
                                                  (.getBytes "UTF-8")))
            transit-reader (transit/reader transit-in :json)
            update-data (transit/read transit-reader)]
        (is (= 200 (:status response)))
        (is (= "Tulowitski" (:last-name update-data))))))

  (testing "PUT non-existent /users/:id returns error"
    (dummy-response channels/update-users {:error {:type :not-found}
                                           :message "No such user"} :error)
    (let [response (http/put (str/join "/" [root-url (java.util.UUID/randomUUID)])
                             {:body (pr-str {})
                              :content-type :edn
                              :accept :edn
                              :throw-exceptions false})
          update-data (clojure.edn/read-string (:body response))]
      (is (= 404 (:status response)))
      (is (= :not-found (get-in update-data [:error :type])))
      (is (= "No such user" (:message update-data))))))

(deftest delete-user-test
  (testing "DELETE to /users/:id of existing user puts delete message on delete-users channel"
    (let [user-id (java.util.UUID/randomUUID)]
      (dummy-response channels/delete-users {:user {:id user-id}})
      (let [response (http/delete (str/join "/" [root-url user-id])
                                  {:accept :edn})
            delete-data (clojure.edn/read-string (:body response))]
        (is (= 200 (:status response)))
        (is (= user-id (:id delete-data))))))

  (testing "DELETE existing /users/:id with Transit+JSON response"
    (let [user-id (java.util.UUID/randomUUID)]
      (dummy-response channels/delete-users {:user {:id user-id}})
      (let [response (http/delete (str/join "/" [root-url user-id])
                                  {:accept :transit+json})
            transit-in (ByteArrayInputStream. (-> response
                                                  :body
                                                  (.getBytes "UTF-8")))
            transit-reader (transit/reader transit-in :json)
            delete-data (transit/read transit-reader)]
        (is (= 200 (:status response)))
        (is (= user-id (:id delete-data))))))

  (testing "DELETE non-existent /users/:id returns error"
    (dummy-response channels/delete-users {:error {:type :not-found}
                                           :message "No such user"} :error)
    (let [response (http/delete (str/join "/" [root-url (java.util.UUID/randomUUID)])
                                {:accept :edn, :throw-exceptions false})
          delete-data (clojure.edn/read-string (:body response))]
      (is (= 404 (:status response)))
      (is (= :not-found (get-in delete-data [:error :type])))
      (is (= "No such user" (:message delete-data))))))

(deftest ping-test
  (testing "ping should respond with 'OK'"
    (let [response (http/get (str/join "/" [root-url "ping"])
                             {:headers {:accept "text/plain"}})]
      (is (= 200 (:status response)))
      (is (= "OK" (:body response))))))

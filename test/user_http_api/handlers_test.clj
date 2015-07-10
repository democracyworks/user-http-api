(ns user-http-api.handlers-test
    (:require [user-http-api.handlers :refer :all]
              [clojure.test :refer :all]))

(deftest ok-test
  (is (= :ok (:status (ok {})))))

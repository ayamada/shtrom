(ns shtrom.core.t-app
  (:use [midje.sweet]
        [shtrom.core.t-data]
        [shtrom.core.t-common])
  (:require [clojure.java.io :as io]
            [ring.mock.request :refer [request query-string body]]
            (shtrom.core [handler :refer [app]]
                         [request :refer [init-request]])))

(defn- prepare
  []
  (let [test1-data-dir (str test-dir "/" test1-key)]
    (.mkdirs (io/file test1-data-dir))
    (io/copy (io/file test-resource-path) (io/file (str test1-data-dir "/" "test-64.bist")))))

(defn- clean-up
  []
  (let [delete-if-exist (fn [f]
                          (let [file (io/file f)]
                            (when (.exists file)
                              (.delete file))))
        test1-data-dir (str test-dir "/" test1-key)
        test2-data-dir (str test-dir "/" test2-key)]
    (delete-if-exist (str test1-data-dir "/" "test-64.bist"))
    (delete-if-exist (str test1-data-dir "/" "test-128.bist"))
    (delete-if-exist test1-data-dir)
    (delete-if-exist (str test2-data-dir "/" "test-64.bist"))
    (delete-if-exist test2-data-dir)))

(with-state-changes [(before :facts (do
                                      (prepare)
                                      (init-request "test.shtrom.config.clj")))
                     (after :facts (clean-up))]
  (fact "read histogram"
        (parse-body
         (app (-> (request :get (format "/%s/%s/%d" test1-key test-ref test-bin-size))
                  (query-string {:start 0
                                 :end 256}))))
        => (just {:body test-hist-body
                  :headers {"Content-Length" (str test-content-length), "Content-Type" "application/octet-stream"}
                  :status 200})
        (app (-> (request :get (format "/%s/%s/%d" "not" "found" test-bin-size))
                 (query-string {:start 0
                                :end 100})))
        => (just {:body ""
                  :headers {"Content-Type" "application/octet-stream"}
                  :status 404})))

(with-state-changes [(after :facts (clean-up))]
  (fact "write histogram"
        (app (-> (request :post (format "/%s/%s/%d" test2-key test-ref test-bin-size))))
        => (just {:body #"\w"
                  :headers {}
                  :status 400})
        (app (-> (request :post (format "/%s/%s/%d" test2-key test-ref test-bin-size))
                 (body (values->bytes (nth test-hist-body 2)))))
        => (just {:body "OK"
                  :headers {}
                  :status 200})
        (parse-body
         (app (-> (request :get (format "/%s/%s/%d" test2-key test-ref test-bin-size))
                  (query-string {:start 0
                                 :end 256}))))
        => (just {:body test-hist-body
                  :headers {"Content-Length" (str test-content-length), "Content-Type" "application/octet-stream"}
                  :status 200})))

(with-state-changes [(before :facts (do
                                      (prepare)
                                      (init-request "test.shtrom.config.clj")))
                     (after :facts (clean-up))]
  (fact "reduce histogram"
        (app (-> (request :post (format "/%s/%s/%d/reduction" test1-key test-ref test-bin-size))))
        => (just {:body "OK"
                  :headers {}
                  :status 200})
        (parse-body
         (app (-> (request :get (format "/%s/%s/%d" test1-key test-ref (* 2 test-bin-size)))
                  (query-string {:start 0
                                 :end 256}))))
        => (just {:body test-reduce-hist-body
                  :headers {"Content-Length" (str test-reduce-content-length), "Content-Type" "application/octet-stream"}
                  :status 200})))

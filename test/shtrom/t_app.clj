(ns shtrom.t-app
  (:require [midje.sweet :refer :all]
            [shtrom.t-fixture :refer :all]
            [shtrom.t-common :refer :all]
            [clojure.java.io :as io]
            [ring.mock.request :refer [request query-string body]]
            (shtrom [handler :refer [app]]
                    [config :refer [load-config]]
                    [cache :refer [prepare-cache!]]
                    [util :refer [delete-if-exists]])))

(defn- prepare
  []
  (let [test1-data-dir (str test-dir "/" test1-key)]
    (.mkdirs (io/file test1-data-dir))
    (io/copy (io/file (str test-resources-dir "/test.bist")) (io/file (str test1-data-dir "/000000-64.bist")))
    (spit (str test1-data-dir "/main.info")
          (pr-str {:count 1
                   :refs {"test" {:name "test"
                                  :index 0}}}))))

(defn- clean-up
  []
  (let [test1-data-dir (str test-dir "/" test1-key)
        test2-data-dir (str test-dir "/" test2-key)]
    (delete-if-exists (str test1-data-dir "/" "main.info"))
    (delete-if-exists (str test1-data-dir "/" "000000-64.bist"))
    (delete-if-exists (str test1-data-dir "/" "000000-128.bist"))
    (delete-if-exists test1-data-dir)
    (delete-if-exists (str test2-data-dir "/" "main.info"))
    (delete-if-exists (str test2-data-dir "/" "000000-64.bist"))
    (delete-if-exists test2-data-dir)
    (delete-if-exists test-dir)))

(defn- load-test-config! []
  (load-config "test.shtrom.config.clj"))

(with-state-changes [(before :facts (do
                                      (prepare)
                                      (load-test-config!)
                                      (prepare-cache!)))
                     (after :facts (clean-up))]
  (fact "read histogram"
    (parse-body
      (app (-> (request :get (format "/%s/%s/%d" test1-key test-ref test-bin-size))
               (query-string {:start 0
                              :end 256}))))
    => (just {:body test-hist-body
              :headers {"Content-Length" (str test-content-length), "Content-Type" "application/octet-stream"}
              :status 200})
    (parse-body
      (app (-> (request :get (format "/%s/%s/%d" test1-key test-ref test-bin-size))
               (query-string {:start 256
                              :end 0}))))
    => (just {:body [0 0 (list)]
              :headers {"Content-Length" "16", "Content-Type" "application/octet-stream"}
              :status 200})
    (app (-> (request :get (format "/%s/%s/%d" "not" "found" test-bin-size))
             (query-string {:start 0
                            :end 100})))
    => (just {:body "Not Found"
              :headers {"Content-Type" "text/plain"}
              :status 404})))

(with-state-changes [(before :facts (do
                                      (load-test-config!)
                                      (prepare-cache!)))
                     (after :facts (clean-up))]
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
    (app (-> (request :post (format "/%s/%s/%d" test2-key test-ref test-small-bin-size))
             (body (values->bytes (nth test-long-hist-body 2)))))
    => (just {:body "OK"
              :headers {}
              :status 200})
    (parse-body
     (app (-> (request :get (format "/%s/%s/%d" test2-key test-ref test-bin-size))
              (query-string {:start 0
                             :end 256}))))
    => (just {:body test-hist-body
              :headers {"Content-Length" (str test-content-length), "Content-Type" "application/octet-stream"}
              :status 200})
    (parse-body
     (app (-> (request :get (format "/%s/%s/%d" test2-key test-ref test-small-bin-size))
              (query-string {:start 0
                             :end 256}))))
    => (just {:body test-long-hist-body
              :headers {"Content-Length" (str test-long-content-length), "Content-Type" "application/octet-stream"}
              :status 200})))

(with-state-changes [(before :facts (do
                                      (prepare)
                                      (load-test-config!)
                                      (prepare-cache!)))
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

(with-state-changes [(before :facts (do
                                      (load-test-config!)
                                      (prepare-cache!)))
                     (after :facts (clean-up))]
  (fact "reduce histogram (invalid file)"
    (app (-> (request :post (format "/%s/%s/%d/reduction" test1-key test-ref test-bin-size))))
    => (just {:body "Not Found"
              :headers {"Content-Type" "text/plain"}
              :status 404})))

(with-state-changes [(before :facts (do
                                      (prepare)
                                      (load-test-config!)
                                      (prepare-cache!)))
                     (after :facts (clean-up))]
  (fact "clear histogram"
    (app (-> (request :post (format "/%s/%s/%d" test1-key test-ref test-bin-size))
             (body (values->bytes (nth test-hist-body 2)))))
    => (just {:body "OK"
              :headers {}
              :status 200})
    (app (request :delete (format "/%s" test1-key)))
    => (just {:body "OK"
              :headers {}
              :status 200})
    (app (-> (request :get (format "/%s/%s/%d" test1-key test-ref test-bin-size))
             (query-string {:start 0
                            :end 100})))
    => (just {:body "Not Found"
              :headers {"Content-Type" "text/plain"}
              :status 404})
    (app (request :delete (format "/notfound")))
    => (just {:body "OK"
              :headers {}
              :status 200})))

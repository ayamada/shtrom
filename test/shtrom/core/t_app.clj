(ns shtrom.core.t-app
  (:use [midje.sweet])
  (:require [clojure.java.io :as io]
            [ring.mock.request :refer [request query-string]]
            (shtrom.core [handler :refer [app]]
                         [request :refer [init-request]]))
  (:import [java.nio ByteBuffer ByteOrder]))

(def ^:private test-data-dir "/tmp/0")

(def test-data-length 32)
(def test-data-body '(0 256 (51406115 305419793 2311186 304231527)))

(defn prepare
  []
  (.mkdirs (io/file test-data-dir))
  (io/copy (io/file "test/resources/test-64.bist") (io/file (str test-data-dir "/test-64.bist"))))

(defn- ^ByteBuffer gen-byte-buffer
  ([]
     (gen-byte-buffer 8))
  ([size]
     (.order (ByteBuffer/allocate size) ByteOrder/BIG_ENDIAN)))

(defn- stream->values
  [s len]
  (let [bytes (byte-array len)
        l (.read s bytes 0 len)
        bb (doto (gen-byte-buffer len)
             (.limit len)
             (.put bytes 0 len)
             (.position 0))
        left (.getLong bb)
        right (.getLong bb)
        values (map (fn [_] (.getInt bb))
                    (range (quot (- len 16) 4)))]
    (println left right values)
    [left right values]))

(defn- parse-body
  [res]
  (let [[left right values] (stream->values (:body res) test-data-length)]
    (assoc res :body (list left right values))))

(with-state-changes [(before :facts (do
                                      (prepare)
                                      (init-request "test.shtrom.config.clj")))]
  (fact "read histogram"
        (parse-body
         (app (-> (request :get "/0/test/64")
                  (query-string {:start 0
                                 :end 256}))))
        => (just {:body test-data-body
                  :headers {"Content-Length" (str test-data-length), "Content-Type" "application/octet-stream"}
                  :status 200})
        (app (query-string
              (request :get "/0/none/64") {:start 0
                                           :end 100}))
        => (just {:body ""
                  :headers {"Content-Type" "application/octet-stream"}
                  :status 404})))

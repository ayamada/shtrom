(ns shtrom.core.t-app
  (:use [midje.sweet])
  (:require [clojure.java.io :as io]
            [ring.mock.request :refer [request query-string body]]
            (shtrom.core [handler :refer [app]]
                         [request :refer [init-request]]))
  (:import [java.nio ByteBuffer ByteOrder]))

(def ^:private test-dir "/tmp")

(def ^:private test1-key "1")
(def ^:private test2-key "2")
(def ^:private test-ref "test")
(def ^:private test-bin-size 64)

(def test-hist-length 4)
(def test-hist-body '(0 256 (51406115 305419793 2311186 304231527)))
(def test-content-length (+ 16 (* test-hist-length 4)))

(def test-resource-path "test/resources/test-64.bist")

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
    (delete-if-exist test1-data-dir)
    (delete-if-exist (str test2-data-dir "/" "test-64.bist"))
    (delete-if-exist test2-data-dir)))

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
    [left right values]))

(defn- values->bytes
  [values]
  (let [len (* 4 (count values))
        bb (doto (gen-byte-buffer len)
             (.limit len))]
    (doseq [v values]
      (.putInt bb v))
    (.position bb 0)
    (.array bb)))

(defn- parse-body
  [res]
  (let [[left right values] (stream->values (:body res) test-content-length)]
    (assoc res :body (list left right values))))


(with-state-changes [(before :facts (do
                                      (prepare)
                                      (init-request "test.shtrom.config.clj")))
                     (after :facts (clean-up))]
  (fact "read histogram"
        (parse-body
         (app (-> (request :get (format "/%s/%s/%d" test1-key test-ref test-bin-size))
                  (query-string {:start 0
                                 :end 256}))))
        => (just {:body test-data-body
                  :headers {"Content-Length" (str test-data-length), "Content-Type" "application/octet-stream"}
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
                 (body (values->bytes (nth test-data-body 2)))))
        => (just {:body "OK"
                  :headers {}
                  :status 200})
        (parse-body
         (app (-> (request :get (format "/%s/%s/%d" test2-key test-ref test-bin-size))
                  (query-string {:start 0
                                 :end 256}))))
        => (just {:body test-data-body
                  :headers {"Content-Length" (str test-data-length), "Content-Type" "application/octet-stream"}
                  :status 200})))

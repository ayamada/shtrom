(ns shtrom.t-client
  (:require [midje.sweet :refer :all]
            [ring.adapter.jetty :as jetty]
            [shtrom.config :as config]
            [shtrom.cache :as cache]
            [shtrom.handler :as handler]
            [shtrom.client :as client]))

(def test-key "0")
(def test-ref "test")
(def test-bin-size 64)
(def test-values [345 127 493 312])

(def long-test-refs ["test-long-a" "test-long-b" "test-long-c" "test-long-d" "test-long-e" "test-long-f" "test-long-g" "test-long-h"])
(def max-value 128)
(def long-test-values (take 100000 (repeatedly #(rand-int max-value))))

(def ^:private bin-sizes [64
                          128
                          256
                          512
                          1024
                          2048
                          4096
                          8192
                          16384
                          32768
                          65536
                          131072
                          262144
                          524288])

(defn concurrent-reduce
  [key refs bin-size initial-values]
  (doseq [r refs]
    (client/save-hist key r bin-size initial-values))
  (doall
   (pmap
    (fn [r]
      (doseq [s bin-sizes]
        (client/reduce-hist key r s)))
    refs))
  nil)

(def ^:private jetty-server (atom nil))

(defn- run-server! []
  (when-not @jetty-server
    (config/load-config "test.shtrom.config.clj")
    (cache/prepare-cache!)
    (let [options {:port client/port
                   :join? false
                   :daemon? true}
          server (jetty/run-jetty #'shtrom.handler/app options)]
      (reset! jetty-server server))))

(defn- shutdown-server! []
  (when @jetty-server
    (.stop @jetty-server)
    (Thread/sleep 1000)
    (reset! jetty-server nil)))

(with-state-changes [(before :facts (do
                                      (client/shtrom-init "test.shtrom-client.config.clj")
                                      (run-server!)))
                     (after :facts (shutdown-server!))]
  (fact "save/load/reduce histogram"
    (client/save-hist test-key test-ref test-bin-size []) => (throws RuntimeException "Empty values")
    (client/save-hist test-key test-ref test-bin-size test-values) => nil
    (client/load-hist "not" "found" test-bin-size 0 256) => [0 0 (list)]
    (client/load-hist test-key test-ref test-bin-size 0 256) => [0 256 test-values]
    (client/load-hist test-key test-ref test-bin-size -1 256) => [0 256 test-values]
    (client/load-hist test-key test-ref test-bin-size 256 0) => [0 0 (list)]
    (client/load-hist test-key test-ref test-bin-size 1 1) => anything
    (client/reduce-hist "not" "found" test-bin-size) => (throws RuntimeException #"Invalid key, ref or bin-size")
    (client/reduce-hist test-key test-ref test-bin-size) => nil
    (client/delete-hist test-key) => nil))

(with-state-changes [(before :facts (do
                                      (client/shtrom-init "test.shtrom-client.config.clj")
                                      (run-server!)))
                     (after :facts (shutdown-server!))]
  (fact "concurrently reduce histogram"
    (concurrent-reduce test-key long-test-refs test-bin-size long-test-values) => nil
    (client/delete-hist test-key) => nil))

(fact "config file not found"
  (client/shtrom-init) => (throws java.lang.RuntimeException))

(with-state-changes [(before :facts (client/shtrom-init "test.shtrom-client.config.clj"))]
  (fact "for server error"
    (client/save-hist test-key test-ref test-bin-size test-values) => nil
    (client/load-hist test-key test-ref test-bin-size 0 256) => nil
    (client/reduce-hist test-key test-ref test-bin-size) => nil
    (client/delete-hist test-key) => nil))

(ns shtrom.t-client
  (:require [midje.sweet :refer :all]
            [ring.adapter.jetty :as jetty]
            [shtrom.t-data :as t-data]
            [shtrom.config :as config]
            [shtrom.cache :as cache]
            [shtrom.handler :as handler]
            [shtrom.client :as client]))

(defn concurrent-reduce
  [key refs bin-size initial-values]
  (doseq [r refs]
    (client/save-hist key r bin-size initial-values))
  (doall
   (pmap
    (fn [r]
      (doseq [s t-data/bin-sizes]
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
    (client/save-hist t-data/client-key t-data/client-ref t-data/client-bin-size []) => (throws RuntimeException "Empty values")
    (client/save-hist t-data/client-key t-data/client-ref t-data/client-bin-size t-data/client-values) => nil
    (client/load-hist "not" "found" t-data/client-bin-size 0 256) => [0 0 (list)]
    (client/load-hist t-data/client-key t-data/client-ref t-data/client-bin-size 0 256) => [0 256 t-data/client-values]
    (client/load-hist t-data/client-key t-data/client-ref t-data/client-bin-size -1 256) => [0 256 t-data/client-values]
    (client/load-hist t-data/client-key t-data/client-ref t-data/client-bin-size 256 0) => [0 0 (list)]
    (client/load-hist t-data/client-key t-data/client-ref t-data/client-bin-size 1 1) => [0 64 t-data/client-values-first]
    (client/reduce-hist "not" "found" t-data/client-bin-size) => (throws RuntimeException #"Invalid key, ref or bin-size")
    (client/reduce-hist t-data/client-key t-data/client-ref t-data/client-bin-size) => nil
    (client/delete-hist t-data/client-key) => nil))

(with-state-changes [(before :facts (do
                                      (client/shtrom-init "test.shtrom-client.config.clj")
                                      (run-server!)))
                     (after :facts (shutdown-server!))]
  (fact "concurrently reduce histogram"
    (concurrent-reduce t-data/client-key t-data/client-long-refs t-data/client-bin-size t-data/client-long-values) => nil
    (client/delete-hist t-data/client-key) => nil))

(fact "config file not found"
  (client/shtrom-init) => (throws java.lang.RuntimeException))

(with-state-changes [(before :facts (client/shtrom-init "test.shtrom-client.config.clj"))]
  (fact "for server error"
    (client/save-hist t-data/client-key t-data/client-ref t-data/client-bin-size t-data/client-values) => nil
    (client/load-hist t-data/client-key t-data/client-ref t-data/client-bin-size 0 256) => nil
    (client/reduce-hist t-data/client-key t-data/client-ref t-data/client-bin-size) => nil
    (client/delete-hist t-data/client-key) => nil))

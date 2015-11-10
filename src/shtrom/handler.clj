(ns shtrom.handler
  (:use compojure.core)
  (:require [clojure.tools.logging :as logging]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [ring.adapter.jetty :as jetty]
            [ring.util.response :as response]
            [shtrom.config :as config]
            [shtrom.cache :as cache]
            [shtrom.data :as data])
  (:gen-class))

(defn- str->int [str]
  (try
    (Integer. (re-find  #"\d+" str))
    (catch Exception e 0)))

(defmacro wrap
  [req & h]
  `(let [start# (. System (nanoTime))
         ret# ~h
         elapsed# (/ (double (- (. System (nanoTime)) start#)) 1000000.0)]
     (logging/info (str {:route (:compojure/route ~req)
                         :elapsed elapsed#
                         :params (:params ~req)}))
     ret#))

(defroutes app-routes
  (GET  "/:key/:ref/:binsize" [key ref binsize start end :as req]
        (wrap req data/read-hist
              key
              ref
              (str->int binsize)
              (str->int start)
              (str->int end)))
  (POST "/:key/:ref/:binsize" [key ref binsize :as req]
        (wrap req data/write-hist
              key
              ref
              (str->int binsize)
              req))
  (POST "/:key/:ref/:binsize/reduction" [key ref binsize :as req]
        (wrap req data/reduce-hist
              key
              ref
              (str->int binsize)))
  (DELETE "/:key" [key :as req]
          (wrap req data/clear-hist key))
  (route/not-found (-> (response/response "Not Found")
                       (response/header "Content-Type" "text/plain")
                       (response/status 404))))

(def app
  (handler/site app-routes))

(defn init
  []
  (config/load-config)
  (cache/prepare-cache!))

(defn -main
  []
  (init)
  (let [port (or config/port 3001)]
    (jetty/run-jetty #'app {:port port :join? false})))

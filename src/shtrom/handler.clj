(ns shtrom.handler
  (:use compojure.core)
  (:require [compojure.handler :as handler]
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

(defroutes app-routes
  (GET  "/:key/:ref/:binsize" [key ref binsize start end] (data/read-hist key
                                                                          ref
                                                                          (str->int binsize)
                                                                          (str->int start)
                                                                          (str->int end)))
  (POST "/:key/:ref/:binsize" {:keys [params] :as req} (data/write-hist (params :key)
                                                                        (params :ref)
                                                                        (str->int (params :binsize))
                                                                        req))
  (POST "/:key/:ref/:binsize/reduction" {:keys [params] :as req} (data/reduce-hist (params :key)
                                                                                   (params :ref)
                                                                                   (str->int (params :binsize))))
  (DELETE "/:key" [key] (data/clear-hist key))
  (route/not-found (-> (response/response "")
                       (response/header "Content-Type" "application/octet-stream"))))

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

(ns shtrom.handler
  (:use compojure.core)
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [shtrom.core :as core]))

(defn- str->int [str]
  (try
    (Integer. (re-find  #"\d+" str))
    (catch Exception e 0)))

(defroutes app-routes
  (context "/:key/:ref/:binsize" [key ref binsize]
           (defroutes hist-routes
             (GET  "/" [start end] (core/read-hist key ref (str->int binsize) (str->int start) (str->int end)))
             (POST "/" req (core/write-hist key ref binsize))))
  (route/not-found "Not Found"))

(def app
  (handler/site app-routes))

(defn init
  []
  (core/init))

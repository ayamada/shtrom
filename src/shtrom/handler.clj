(ns shtrom.handler
  (:use compojure.core)
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [shtrom.core :as core]))

(defroutes app-routes
  (context "/:key/:ref/:binsize" [key ref binsize]
           (defroutes hist-routes
             (GET  "/" [start end] (core/read-hist key ref binsize start end))
             (POST "/" req (core/write-hist key ref binsize))))
  (route/not-found "Not Found"))

(def app
  (handler/site app-routes))

(defn init
  []
  (core/init))

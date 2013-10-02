(ns shtrom.core.handler
  (:use compojure.core)
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [shtrom.core.request :refer [read-hist write-hist reduce-hist init-request]]))

(defn- str->int [str]
  (try
    (Integer. (re-find  #"\d+" str))
    (catch Exception e 0)))

(defroutes app-routes
  (context "/:key/:ref/:binsize" [key ref binsize]
           (defroutes hist-routes
             (GET  "/" [start end] (read-hist key
                                              ref
                                              (str->int binsize)
                                              (str->int start)
                                              (str->int end)))
             (POST "/" req (write-hist key
                                       ref
                                       (str->int binsize)
                                       req))
             (POST "/reduction" req (reduce-hist key
                                                 ref
                                                 (str->int binsize)))))
  (route/not-found "Not Found"))

(def app
  (handler/site app-routes))

(defn init
  []
  (init-request))

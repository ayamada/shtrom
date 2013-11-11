(ns shtrom.core.handler
  (:use compojure.core)
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [ring.util.response :as response]
            [shtrom.core.request :refer [read-hist write-hist reduce-hist clear-hist init-request]]))

(defn- str->int [str]
  (try
    (Integer. (re-find  #"\d+" str))
    (catch Exception e 0)))

(defroutes app-routes
  (GET  "/:key/:ref/:binsize" [key ref binsize start end] (read-hist key
                                                                     ref
                                                                     (str->int binsize)
                                                                     (str->int start)
                                                                     (str->int end)))
  (POST "/:key/:ref/:binsize" {:keys [params] :as req} (write-hist (params :key)
                                                                   (params :ref)
                                                                   (str->int (params :binsize))
                                                                   req))
  (POST "/:key/:ref/:binsize/reduction" {:keys [params] :as req} (reduce-hist (params :key)
                                                                              (params :ref)
                                                                              (str->int (params :binsize))))
  (DELETE "/:key" [key] (clear-hist key))
  (route/not-found (-> (response/response "")
                       (response/header "Content-Type" "application/octet-stream"))))

(def app
  (handler/site app-routes))

(defn init
  []
  (init-request))

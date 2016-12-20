(ns shtrom.handler
  (:use compojure.core)
  (:require [clojure.tools.logging :as logging]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [compojure.coercions :as coercions]
            [ring.adapter.jetty :as jetty]
            [ring.util.response :as response]
            [ring.middleware.defaults :as ring-defaults]
            [shtrom.config :as config]
            [shtrom.cache :as cache]
            [shtrom.data :as data]
            [shtrom.gz-store :as gz-store]
            [shtrom.error :as error])
  (:gen-class))

(defn- log-time-middleware [handler]
  (fn [request]
    (let [start (. System (nanoTime))
          ret (handler request)
          elapsed (/ (double (- (. System (nanoTime)) start)) 1000000.0)]
      (logging/info (str {:route (:compojure/route request)
                          :elapsed elapsed
                          :params (:params request)}))
      ret)))

(defroutes app-routes
  (GET  "/:key/:ref/:binsize"
        [key :<< not-empty
         ref :<< not-empty
         binsize :<< coercions/as-int
         start :<< coercions/as-int
         end :<< coercions/as-int
         :as req]
        (data/read-hist key ref binsize start end))
  (POST "/:key/:ref/:binsize"
        [key :<< not-empty
         ref :<< not-empty
         binsize :<< coercions/as-int
         :as req]
        (data/write-hist key ref binsize req))
  (POST "/:key/:ref/:binsize/reduction"
        [key :<< not-empty
         ref :<< not-empty
         binsize :<< coercions/as-int
         :as req]
        (data/reduce-hist key ref binsize))
  (POST "/:key"
        [key :<< not-empty
         :as req]
        (data/create-bucket! key))
  (PUT "/:key"
       [key :<< not-empty
        :as req]
       (data/build-bucket! key))
  (DELETE "/:key"
          [key :<< not-empty
           :as req]
          (data/clear-hist key))
  (route/not-found (error/bad-arg-error "No matching routes")))

(def app
  (-> app-routes
      (wrap-routes log-time-middleware)
      (ring-defaults/wrap-defaults (-> ring-defaults/api-defaults
                                       (assoc-in [:responses :content-types] nil)))))

(defn init
  []
  (config/load-config)
  (cache/prepare-cache!))

(defn term
  []
  (gz-store/delete-all-cache-entries!))

(defn -main
  []
  (init)
  (.addShutdownHook (Runtime/getRuntime) (Thread. ^Runnable term))
  (let [port (or config/port 3001)]
    (jetty/run-jetty #'app {:port port :join? false})))

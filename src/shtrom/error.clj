(ns shtrom.error
  (:require [shtrom.util :as util]))

(defn- error-response [error & [status]]
  (util/json-response {:error error} (or status 400)))

(def ^:private illegal-state "IllegalState")
(def ^:private bad-arg "BadArgument")

(defn illegal-state-error [state & [description]]
  (error-response
   (cond-> {:code 1000
            :type illegal-state
            :state state}
     description (assoc :description description))))

(defn not-exists-error []
  (error-response
   {:code 1100
    :type illegal-state
    :description "Bucket does not exist"}
   404))

(defn already-exists-error [state]
  (error-response
   {:code 1200
    :type illegal-state
    :state state
    :description "Bucket already exists"}))

(defn cannot-write-error [state]
  (error-response
   {:code 1300
    :type illegal-state
    :state state
    :description (case state "available" "Bucket is read-only" nil)}))

(defn cannot-read-error [state]
  (error-response
   {:code 1400
    :type illegal-state
    :state state
    :description (case state "created" "Bucket is write-only" nil)}))

(defn bad-arg-error [& [description]]
  (error-response
   (cond-> {:code 2000
            :type bad-arg}
     description (assoc :description description))))

(defn bad-ref-error []
  (error-response
   {:code 2100
    :type bad-arg
    :description "Reference does not exist"}
   404))


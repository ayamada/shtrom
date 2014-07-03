(ns shtrom.cache
  (:require [clojure.java.io :as io]
            [shtrom.config :as config]))

(defn prepare-cache!
  []
  (let [d (io/file config/data-dir)]
    (when-not (.exists d)
      (.mkdirs d))))

(defn cache-path
  [key]
  (str config/data-dir "/" key))

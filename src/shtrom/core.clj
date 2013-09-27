(ns shtrom.core
  (:require [shtrom.util :refer [bist-read]]))

(declare data-dir)

(defn init
  []
  (let [config (read-string (slurp "config/shtrom.config.clj"))]
    (def data-dir (:data-dir config))))

(defn hist-path
  [key ref binsize]
  (format "%s/%s/%s-%s.bist" data-dir key ref binsize))

(defn read-hist
  [key ref binsize start end]
  (let [left (int (quot start binsize))
        right (inc (quot end binsize))
        path (hist-path key ref binsize)]
    (pr-str (bist-read path left right))))

(defn write-hist
  [key ref binsize]
  "done")

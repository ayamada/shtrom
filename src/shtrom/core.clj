(ns shtrom.core
  (:require [ring.util.response :as response]
            [shtrom.util :refer [bist-read data->byte-array]]))

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
        path (hist-path key ref binsize)
        data (bist-read path left right)]
    (-> (response/response (new java.io.ByteArrayInputStream (data->byte-array data)))
        (response/content-type (:content-type "application/octet-stream"))
        (response/header "Content-Length" (* 4 (count data))))))

(defn write-hist
  [key ref binsize]
  "done")

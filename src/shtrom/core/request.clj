(ns shtrom.core.request
  (:require [ring.util.response :as response]
            [shtrom.core.util :refer [prepare-file bist-read bist-write
                                      data->byte-array byte-array->data http-body->bytes
                                      reduce-values]]))

(declare data-dir)

(defn init-request
  []
  (let [config (read-string (slurp "config/shtrom.config.clj"))]
    (def data-dir (:data-dir config))))

(defn hist-path
  [key ref binsize]
  (format "%s/%s/%s-%s.bist" data-dir key ref binsize))

(defn read-hist
  [key ref binsize start end]
  (try
    (let [left (int (quot start binsize))
          right (inc (quot end binsize))
          path (hist-path key ref binsize)
          data (bist-read path left right)]
      (-> (response/response (new java.io.ByteArrayInputStream (data->byte-array data)))
          (response/content-type (:content-type "application/octet-stream"))
          (response/header "Content-Length" (* 4 (count data)))))
    (catch java.io.FileNotFoundException e (do
                                             (println (format "read-hist: file not found: %s %s %d %d %d" key ref binsize start end))
                                             nil))
    (catch java.io.EOFException e (do
                                    (println (format "read-hist: eof: %s %s %d %d %d" key ref binsize start end))
                                    nil))))

(defn write-hist
  [key ref binsize req]
  (let [len (:content-length req)
        body (http-body->bytes (:body req) len)
        values (byte-array->data body len)
        path (hist-path key ref binsize)]
    (prepare-file path)
    (bist-write path values)
    "OK"))

(defn reduce-hist
  [key ref binsize]
  (try
    (let [path (hist-path key ref binsize)
          values (bist-read path)
          new-values (reduce-values values)
          new-path (hist-path key ref (* binsize 2))]
      (prepare-file new-path)
      (bist-write new-path new-values)
      "OK")
    (catch java.io.FileNotFoundException e (do
                                             (println (format "reduce-hist: file not found: %s %s %d" key ref binsize))
                                             nil))
    (catch java.io.EOFException e (do
                                    (println (format "reduce-hist: eof: %s %s %d" key ref binsize))
                                    nil))))

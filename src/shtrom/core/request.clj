(ns shtrom.core.request
  (:require [ring.util.response :as response]
            [shtrom.core.util :refer [prepare-file bist-read bist-write
                                      values->content values->content-length
                                      byte-array->data http-body->bytes
                                      reduce-values]]))

(declare data-dir)

(defn init-request
  []
  (let [config (read-string (slurp "config/shtrom.config.clj"))]
    (def data-dir (:data-dir config))))

(defn hist-path
  [key ref bin-size]
  (format "%s/%s/%s-%s.bist" data-dir key ref bin-size))

(defn read-hist
  [key ref bin-size start end]
  (try
    (let [left (int (quot start bin-size))
          right (inc (quot end bin-size))
          path (hist-path key ref bin-size)
          [l r values] (bist-read path left right)]
      (-> (response/response (new java.io.ByteArrayInputStream
                                  (values->content (* l bin-size)
                                                   (* r bin-size)
                                                   values)))
          (response/content-type (:content-type "application/octet-stream"))
          (response/header "Content-Length" (values->content-length values))))
    (catch java.io.FileNotFoundException e (do
                                             (println (format "read-hist: file not found: %s %s %d %d %d" key ref bin-size start end))
                                             nil))
    (catch java.io.EOFException e (do
                                    (println (format "read-hist: eof: %s %s %d %d %d" key ref bin-size start end))
                                    nil))))

(defn write-hist
  [key ref bin-size req]
  (let [len (:content-length req)
        body (http-body->bytes (:body req) len)
        values (byte-array->data body len)
        path (hist-path key ref bin-size)]
    (prepare-file path)
    (bist-write path values)
    "OK"))

(defn reduce-hist
  [key ref bin-size]
  (try
    (let [path (hist-path key ref bin-size)
          values (bist-read path)
          new-values (reduce-values values)
          new-path (hist-path key ref (* bin-size 2))]
      (prepare-file new-path)
      (bist-write new-path new-values)
      "OK")
    (catch java.io.FileNotFoundException e (do
                                             (println (format "reduce-hist: file not found: %s %s %d" key ref bin-size))
                                             nil))
    (catch java.io.EOFException e (do
                                    (println (format "reduce-hist: eof: %s %s %d" key ref bin-size))
                                    nil))))

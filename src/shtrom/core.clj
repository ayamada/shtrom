(ns shtrom.core)

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
  (let [left (quot start binsize)
        right (inc (quot end binsize))
        path (hist-path key ref binsize)]
    ))

(defn write-hist
  [key ref binsize]
  "done")

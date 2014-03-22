(ns shtrom.core.request
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as logging]
            [ring.util.response :as response]
            [shtrom.core.util :refer [prepare-file bist-read bist-write
                                      values->content values->content-length
                                      byte-array->data http-body->bytes
                                      reduce-values
                                      file-size delete-if-exists list-files]]))

(declare data-dir)

(def ^:private default-config-filename "shtrom.config.clj")

;; response

(defn- success
  [msg]
  (-> (response/response msg)
      (response/status 200)))

(defn- abort-bad-request
  [msg]
  (-> (response/response msg)
      (response/status 400)))

;; util

(defn init-request
  ([]
     (init-request default-config-filename))
  ([f]
     (let [rsrc (let [f-classpath (io/resource f)
                      f-etc (io/file (str "/etc/" f))]
                  (cond
                   (not (nil? f-classpath)) f-classpath
                   (.isFile f-etc) f-etc
                   :else nil))
           conf (if (nil? rsrc)
                  (throw (RuntimeException. (str "Configuration file not found: " f)))
                  (read-string (slurp rsrc)))]
       (def data-dir (:data-dir conf))
       (def port (:port conf)))))

(defn hist-dir
  [key]
  (format "%s/%s" data-dir key))

(defn hist-path
  [key ref bin-size]
  (format "%s/%s/%s-%s.bist" data-dir key ref bin-size))

(defn wait-for-availability
  ([f & {:keys [size count]
         :or {size -1
              count 3}}]
     (let [check-fn (if (neg? size)
                      (fn []
                        (try
                          (pos? (file-size f))
                          (catch Exception e false)))
                      (fn []
                        (try
                          (= size (file-size f))
                          (catch Exception e false))))]
       (loop [available (check-fn)
              c 0]
         (when (and (not available)
                    (< c count))
           (Thread/sleep 1000)
           (recur (check-fn) (inc c)))))))

;; handle

(defn read-hist
  [key ref bin-size start end]
  (wait-for-availability (hist-path key ref bin-size))
  (try
    (let [left (int (quot start bin-size))
          right (inc (quot end bin-size))
          path (hist-path key ref bin-size)
          [l r values] (bist-read path left right)]
      (-> (response/response (new java.io.ByteArrayInputStream
                                  (values->content (* l bin-size)
                                                   (* r bin-size)
                                                   values)))
          (response/header "Content-Length" (values->content-length values))
          (response/header "Content-Type" "application/octet-stream")))
    (catch java.io.FileNotFoundException e (do
                                             (logging/warn (format "read-hist: file not found: %s %s %d %d %d" key ref bin-size start end))
                                             nil))
    (catch java.io.EOFException e (do
                                    (logging/warn (format "read-hist: eof: %s %s %d %d %d" key ref bin-size start end))
                                    nil))))

(defn write-hist
  [key ref bin-size req]
  (let [len (if (nil? (:content-length req))
              0
              (:content-length req))
        body (if (nil? (:body req))
               (byte-array 0)
               (http-body->bytes (:body req) len))
        values (byte-array->data body len)
        path (hist-path key ref bin-size)]
    (prepare-file path)
    (if (pos? (count values))
      (do
        (bist-write path values)
        (success "OK"))
      (do (logging/warn (format "write-hist: bad request: %s %s %d" key ref bin-size))
          (abort-bad-request "Request values are empty")))))

(defn reduce-hist
  [key ref bin-size]
  (wait-for-availability (hist-path key ref bin-size))
  (try
    (let [path (hist-path key ref bin-size)
          [_ _ values] (bist-read path)
          new-values (reduce-values values)
          new-path (hist-path key ref (* bin-size 2))
          new-size (* 4 (count new-values))]
      (prepare-file new-path)
      (bist-write new-path new-values)
      (wait-for-availability new-path :size new-size)
      (success "OK"))
    (catch java.io.FileNotFoundException e (do
                                             (logging/warn (format "reduce-hist: file not found: %s %s %d" key ref bin-size))
                                             nil))
    (catch java.io.EOFException e (do
                                    (logging/warn (format "reduce-hist: eof: %s %s %d" key ref bin-size))
                                    nil))))

(defn clear-hist
  [key]
  (try
    (let [dir-path (hist-dir key)]
      (doseq [f (list-files dir-path)]
        (delete-if-exists (str dir-path "/" f)))
      (delete-if-exists dir-path)
      (success "OK"))
    (catch java.io.FileNotFoundException e (success "OK"))))

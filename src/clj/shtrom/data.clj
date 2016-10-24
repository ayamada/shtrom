(ns shtrom.data
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as logging]
            [ring.util.response :as response]
            [shtrom.util :refer [prepare-file bist-read bist-write
                                 values->content values->content-length
                                 byte-array->data http-body->bytes
                                 reduce-values
                                 file-size delete-if-exists list-files]]
            [shtrom.cache :refer [cache-path ref-key]]
            [shtrom.error :as error]
            [clojure.edn :as edn])
  (:import [shtrom.data IO]))

;;; response

(defn- success
  [msg]
  (-> (response/response msg)
      (response/status 200)))

(defn- abort-bad-request
  [msg]
  (-> (response/response msg)
      (response/status 400)))

;;; util

(def hist-dir cache-path)

(defn hist-path
  [key ref bin-size]
  (str (hist-dir key) "/" ref "-" bin-size ".bist"))

(defn info-path
  [key]
  (str (hist-dir key) "/main.info"))

(defn- wait-for-availability
  [f & {:keys [size count]
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
        (recur (check-fn) (inc c))))))

;;; bucket info

(defn load-info!
  [f & {:keys [force]
        :or {force false}}]
  (let [fp (io/file f)]
    (if (.exists fp)
      (edn/read-string (slurp f))
      (if force
        (let [new-info {:count 0
                        :refs {}
                        :state :created}]
          (spit f (pr-str new-info))
          new-info)
        nil))))

(defn add-ref-info!
  [f info name]
  (let [idx (:count info)
        new-ref {:name name
                 :index idx}
        new-refs (assoc (:refs info) name new-ref)
        new-info (assoc info
                   :count (inc idx)
                   :refs new-refs)]
    (spit f (pr-str new-info))
    new-ref))

(defn write-info!
  [f info]
  (spit f info))

;;; handlers

(defn read-hist
  ""
  [key rname bin-size start end]
  (if-let [info (load-info! (info-path key))]
    (if (= (:state info) :available)
      (let [ref (-> info
                    :refs
                    (get rname nil))
            rkey (ref-key (:index ref))]
        (if rkey
          (do (wait-for-availability (hist-path key rkey bin-size))
              (try
                (let [left (int (quot start bin-size))
                      right (inc (quot end bin-size))
                      path (hist-path key rkey bin-size)
                      [l r values] (bist-read path left right)]
                  (-> (response/response (new java.io.ByteArrayInputStream
                                              (values->content (* l bin-size)
                                                               (* r bin-size)
                                                               values)))
                      (response/header "Content-Length" (values->content-length values))
                      (response/header "Content-Type" "application/octet-stream")))
                (catch java.io.FileNotFoundException e
                  (do
                    (logging/warn (format "read-hist: file not found: %s %s %d %d %d" key rname bin-size start end))
                    (error/bad-arg-error)))
                (catch java.io.EOFException e
                  (do
                    (logging/warn (format "read-hist: eof: %s %s %d %d %d" key rname bin-size start end))
                    (error/bad-arg-error)))))
          (error/bad-ref-error)))
      (error/cannot-read-error (:state info)))
    (error/not-exists-error)))

(defn create-bucket!
  [key]
  (let [path (info-path key)]
    (if-let [info (load-info! path :force false)]
      (error/already-exists-error (:state info))
      (do (prepare-file path)
          (load-info! path :force true)
          (success "Created")))))

(defn build-bucket!
  [key]
  (let [path (info-path key)]
    (if-let [info (load-info! path :force false)]
      (if (= (:state info) :created)
        (do (write-info! path (assoc info :state :available))
            (success "Built"))
        (error/cannot-write-error (:state info)))
      (error/not-exists-error))))

(defn write-hist
  ""
  [key rname bin-size req]
  (let [len (if (nil? (:content-length req))
              0
              (:content-length req))
        body (if (nil? (:body req))
               (byte-array 0)
               (http-body->bytes (:body req) len))
        values (byte-array->data body len)
        info-path (info-path key)]
    (if (pos? (count values))
      (let [_ (prepare-file info-path)
            info (load-info! info-path :force true)]
        (if info
          (if (= (:state info) :created)
            (let [ref-info (let [i (-> info
                                       :refs
                                       (get rname nil))]
                             (if i
                               i
                               (add-ref-info! info-path info rname)))
                  rkey (ref-key (:index ref-info))
                  path (hist-path key rkey bin-size)]
              (if (and ref-info rkey path)
                (do (bist-write path values)
                    (success "OK"))
                (error/bad-ref-error)))
            (error/cannot-write-error (:state info)))
          (error/not-exists-error)))
      (error/bad-arg-error))))

(defn reduce-hist
  ""
  [key rname bin-size]
  (if-let [info (load-info! (info-path key))]
    (if (= (:state info) :created)
      (let [ref (-> info
                    :refs
                    (get rname nil))
            rkey (ref-key (:index ref))]
        (if rkey
          (do
            (wait-for-availability (hist-path key rkey bin-size))
            (try
              (let [path (hist-path key rkey bin-size)
                    [_ _ values] (bist-read path)
                    new-values (reduce-values values)
                    new-path (hist-path key rkey (* bin-size 2))
                    new-size (* 4 (count new-values))]
                (prepare-file new-path)
                (bist-write new-path new-values)
                (wait-for-availability new-path :size new-size)
                (success "OK"))
              (catch java.io.FileNotFoundException e
                (do
                  (logging/warn (format "reduce-hist: file not found: %s %s %d" key rname bin-size))
                  (error/bad-arg-error)))
              (catch java.io.EOFException e
                (do
                  (logging/warn (format "reduce-hist: eof: %s %s %d" key rname bin-size))
                  (error/bad-arg-error)))))
          (error/bad-ref-error)))
      (error/cannot-write-error (:state info)))
    (error/not-exists-error)))

(defn clear-hist
  ""
  [key]
  (let [dir-path (hist-dir key)]
    (if-let [info (load-info! (info-path key) :force false)]
      (do (doseq [f (list-files dir-path)]
            (delete-if-exists (str dir-path "/" f)))
          (delete-if-exists dir-path)
          (success "OK"))
      (error/not-exists-error))))

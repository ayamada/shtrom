(ns shtrom.util
  (require [clojure.java.io :as io]
           [clojure.string :as str])
  (:import [java.nio ByteBuffer ByteOrder]
           [java.io DataInputStream DataOutputStream FileInputStream FileOutputStream IOException EOFException]))

(defn- ^ByteBuffer gen-byte-buffer
  ([]
     (gen-byte-buffer 8))
  ([size]
     (.order (ByteBuffer/allocate size) ByteOrder/BIG_ENDIAN)))

;; reader

(defn- breader
  [^String path]
  (DataInputStream. (FileInputStream. (io/file path))))

(defn skip
  [^DataInputStream rdr ^Integer n]
  (.skipBytes rdr n)
  nil)

(defn- read-bytes
  ([^DataInputStream rdr ^Integer l]
     (let [ba (byte-array l)]
       (.read rdr ba 0 l)
       ba))
  ([^DataInputStream rdr buffer offset ^Integer l]
     (loop [total-read 0]
       (when (< total-read l)
         (let [n (.read rdr buffer (+ offset total-read) (- l total-read))]
           (if (neg? n)
             (throw (EOFException. "Premature EOF"))
             (recur (+ total-read n))))))))

(defn- read-byte-buffer
  [^DataInputStream rdr ^ByteBuffer bb l]
  {:pre (< l (.capacity bb))}
  (read-bytes rdr (.array bb) 0 l)
  (.limit bb (.capacity bb))
  (.position bb l))

(defn- bread-integer
  [rdr]
  (let [bb (gen-byte-buffer)]
    (read-byte-buffer rdr bb 4)
    (.flip bb)
    (.getInt bb)))

;; writer

(defn- bwriter
  [^String  path]
  (DataOutputStream. (FileOutputStream. (io/file path))))

(defn- bwrite-integer
  [wtr val]
  (let [bb (gen-byte-buffer)]
    (.putInt bb val)
    (.write wtr (.array bb) 0 4)))

;; public

(defn bist-read
  [^String f ^Integer start ^Integer end]
  (with-open [rdr (breader f)]
    (skip rdr (* start 4))
    (doall (map (fn [_] (bread-integer rdr))
                (range start end)))))

(defn bist-write
  [^String f values]
  (with-open [wtr (bwriter f)]
    (doseq [v values] (bwrite-integer wtr v))))

(defn data->byte-array
  [data]
  (let [bb (gen-byte-buffer (* 4 (count data)))]
    (doseq [d data]
      (.putInt bb d))
    (.array bb)))

(defn byte-array->data
  [bytes len]
  (let [bb (doto (gen-byte-buffer len)
             (.put bytes 0 len)
             (.position 0))
        data-len (quot len 4)]
    (map (fn [_] (.getInt bb))
         (range data-len))))

(defn prepare-file
  [path]
  (let [f (io/file path)]
    (when-not (.exists f)
      (let [dir-path (.getParent f)
            dir-f (io/file dir-path)]
        (.mkdirs dir-f)))))

;; http

(defn http-body->bytes
  [input len]
  (let [bytes (byte-array len)]
    (.read input bytes 0 len)
    bytes))

;; convert utility

(defn hist->bist
  [hist-path]
  (let [f (io/file hist-path)
        name (.getName f)
        dir (.getParent f)
        ext (re-find #"\.[0-9a-z]+$" name)
        new-name (str/replace name #"\.hist$" ".bist")
        bist-path (format "%s/%s" dir new-name)]
    (when-not (= ".hist" ext)
      (throw (Exception. (str "Invalid file extension: " ext))))
    (with-open [rdr (io/reader hist-path)]
      (let [values (pmap (fn [l]
                           (int (:val (read-string l))))
                         (line-seq rdr))]
        (with-open [wtr (bwriter bist-path)]
          (doseq [v values] (bwrite-integer wtr v)))))
    nil))

(defn dir-hist->bist
  [dir-path]
  (let [dir (io/file dir-path)]
    (doseq [f (file-seq dir)]
      (when (= ".hist" (re-find #"\.[0-9a-z]+$" (.getName f)))
        (hist->bist f)))))

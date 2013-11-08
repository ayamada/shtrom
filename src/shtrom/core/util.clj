(ns shtrom.core.util
  (require [clojure.java.io :as io]
           [clojure.string :as str])
  (:import [java.nio ByteBuffer ByteOrder]
           [java.io DataInputStream DataOutputStream FileInputStream FileOutputStream IOException EOFException]))

(defn- ^ByteBuffer gen-byte-buffer
  ([]
     (gen-byte-buffer 8))
  ([size]
     (.order (ByteBuffer/allocate size) ByteOrder/BIG_ENDIAN)))

(defn- validate-index
  [i len]
  (cond
   (< i 0)  0
   (> i len) len
   :else i))

;; reader

(defn file-size
  [^String path]
  (let [f (io/file path)]
    (when-not (.exists f)
      (throw (java.io.FileNotFoundException.)))
    (.length f)))

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
  ([^String f]
     (let [len (quot (file-size f) 4)]
       (with-open [rdr (breader f)]
         [0
          len
          (doall (map (fn [i]
                        (bread-integer rdr))
                      (range 0 len)))])))
  ([^String f ^Integer start ^Integer end]
     (let [len (quot (file-size f) 4)
           left (validate-index start len)
           right (validate-index end len)]
       (if (< left right)
         (with-open [rdr (breader f)]
           (skip rdr (* left 4))
           [left
            right
            (doall (map (fn [i]
                          (bread-integer rdr))
                        (range left right)))])
         [0 0 (list)]))))

(defn bist-write
  [^String f values]
  (with-open [wtr (bwriter f)]
    (doseq [v values] (bwrite-integer wtr v))))

(defn values->content-length
  [values]
  (+ 16 (* 4 (count values))))

(defn values->content
  [start end values]
  (let [bb (gen-byte-buffer (values->content-length values))]
    (.putLong bb start)
    (.putLong bb end)
    (doseq [v values]
      (.putInt bb v))
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
  (let [bb (gen-byte-buffer len)
        data-size 4096
        data (byte-array 4096)]
    (loop [l (.read input data 0 data-size)]
      (if (neg? l)
        (.array bb)
        (do
          (.put bb data 0 l)
          (recur (.read input data 0 data-size)))))))

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

(defn reduce-values
  [values]
  (map (fn [v] (apply + v)) (partition-all 2 values)))

(ns shtrom.util
  (require [clojure.java.io :as io]
           [clojure.string :as str])
  (:import [java.nio ByteBuffer ByteOrder]
           [java.io DataInputStream DataOutputStream FileInputStream FileOutputStream IOException EOFException]))

(defn- ^ByteBuffer gen-byte-buffer []
  (.order (ByteBuffer/allocate 8) ByteOrder/BIG_ENDIAN))

(defn- bwriter
  [path]
  (DataOutputStream. (FileOutputStream. (io/file path))))

(defn- bwrite-integer
  [wtr val]
  (let [bb (gen-byte-buffer)]
    (.putInt bb val)
    (.write wtr (.array bb) 0 4)))

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
                           (int (:val (read-string l)))) (line-seq rdr))]
        (with-open [wtr (bwriter bist-path)]
          (doseq [v values] (bwrite-integer wtr v)))))
    nil))

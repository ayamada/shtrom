(ns shtrom.util
  (require [clojure.java.io :as io]
           [clojure.string :as str]
           [cheshire.core :as cheshire])
  (:import [java.nio ByteBuffer ByteOrder]
           (java.io File InputStream DataInputStream DataOutputStream
                    FileInputStream FileOutputStream
                    ByteArrayInputStream ByteArrayOutputStream
                    IOException EOFException)
           [java.util.zip GZIPOutputStream]
           [shtrom Util]))

(defn- ^ByteBuffer gen-byte-buffer
  ([]
     (Util/genByteBuffer 8))
  ([size]
     (Util/genByteBuffer size)))

(defn- validate-index
  [i len]
  (cond
   (< i 0)  0
   (> i len) len
   :else i))

;;; filesystem

(defn delete-if-exists
  [f]
  (let [file (io/file f)]
    (when (.exists file)
      (.delete file))))

(defn list-files
  [d]
  (let [dir (io/file d)]
    (when (.exists dir)
      (seq (.list dir)))))

(defn file-size
  [^String path]
  (let [f (io/file path)]
    (when-not (.exists f)
      (throw (java.io.FileNotFoundException.)))
    (.length f)))

;;; reader

(defn- breader
  ^DataInputStream [^String path]
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

;;; writer

(defn- bwriter
  ^DataOutputStream [^String  path]
  (DataOutputStream. (FileOutputStream. (io/file path))))

(defn- bwrite-integer
  [^DataOutputStream wtr val]
  (let [bb (gen-byte-buffer)]
    (.putInt bb val)
    (.write wtr (.array bb) 0 4)))

;;; public

(defn bist-read
  ([^String path]
     (let [f (io/file path)]
       (when-not (.exists f)
         (throw (java.io.FileNotFoundException.)))
       (let [len (quot (file-size f) 4)]
         [0 len (Util/bistRead f)])))
  ([^String path ^Integer start ^Integer end]
     (let [f (io/file path)]
       (when-not (.exists f)
         (throw (java.io.FileNotFoundException.)))
       (let [len (quot (file-size f) 4)
             left (validate-index start len)
             right (validate-index end len)]
       (if (< left right)
         [left right (Util/bistReadWithRange f left right)]
         [0 0 (int-array nil)])))))

(defn bist-write
  [^String path ^"[I" values]
  (Util/bistWrite (io/file path) values))

(defn values->content-length
  [^"[I" values]
  (Util/valuesToContentLength values))

(defn values->content
  [start end ^"[I" values]
  (Util/valuesToContent start end values))

(defn byte-array->data
  [^"[B" bs len]
  (Util/byteArrayToData bs len))

(defn prepare-file
  [path]
  (let [f (io/file path)]
    (when-not (.exists f)
      (let [dir-path (.getParent f)
            dir-f (io/file dir-path)]
        (.mkdirs dir-f)))))

;;; http

(defn http-body->bytes
  [^InputStream input len]
  (Util/httpBodyToBytes input len))

;;; conversion utility

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
      (let [values (int-array (pmap (fn [l]
                                      (int (:val (read-string l))))
                                    (line-seq rdr)))]
        (bist-write bist-path values)))
    nil))

(defn dir-hist->bist
  [dir-path]
  (let [dir (io/file dir-path)]
    (doseq [^File f (file-seq dir)]
      (when (= ".hist" (re-find #"\.[0-9a-z]+$" (.getName f)))
        (hist->bist f)))))

(defn reduce-values
  [^"[I" values]
  (Util/reduce values))

(defn- gzip
  [^String s]
  (with-open [bout (ByteArrayOutputStream.)]
    (with-open [out (GZIPOutputStream. bout)]
      (.write out (.getBytes s)))
    (ByteArrayInputStream. (.toByteArray bout))))

(defn json-response
  [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/json"
             "Content-Encoding" "gzip"}
   :body (gzip (cheshire/generate-string data))})

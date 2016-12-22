(ns shtrom.util
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [cheshire.core :as cheshire]
            [shtrom.gz-store :as gz-store])
  (:import [java.io File InputStream ByteArrayInputStream ByteArrayOutputStream]
           [java.util.zip GZIPOutputStream]
           [shtrom BistReader #_BistWriter]
           [shtrom.util IOUtil]))

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

;;; public

(defn bist-read
  ([^String path]
     (gz-store/gunzip-bist! path)
     (let [f (io/file path)]
       (let [len (quot (file-size f) 4)
             br (BistReader. path)]
         [0 len (.read br)])))
  ([^String path ^Integer start ^Integer end]
     (gz-store/gunzip-bist! path)
     (let [f (io/file path)]
       (let [len (quot (file-size f) 4)
             left (validate-index start len)
             right (validate-index end len)
             br (BistReader. path)]
       (if (< left right)
         [left right (.readWithRange br left right)]
         [0 0 (int-array nil)])))))

(defn bist-write
  [^String path ^"[I" values]
  (gz-store/gzip-bist! path values))

(defn values->content-length
  [^"[I" values]
  (IOUtil/valuesToContentLength values))

(defn values->content
  [start end ^"[I" values]
  (IOUtil/valuesToContent start end values))

(defn byte-array->data
  [^"[B" bs len]
  (IOUtil/byteArrayToData bs len))

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
  (IOUtil/httpBodyToBytes input len))

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
  (IOUtil/reduce values))

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

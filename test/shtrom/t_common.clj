(ns shtrom.t-common
  (:import [java.nio ByteBuffer ByteOrder]))

(defn- ^ByteBuffer gen-byte-buffer
  ([]
     (gen-byte-buffer 8))
  ([size]
     (.order (ByteBuffer/allocate size) ByteOrder/BIG_ENDIAN)))

(defn- str->int [str]
  (try
    (Integer. (re-find  #"\d+" str))
    (catch Exception e 0)))

(defn- stream->values
  [s len]
  (let [bytes (byte-array len)
        l (.read s bytes 0 len)
        bb (doto (gen-byte-buffer len)
             (.limit len)
             (.put bytes 0 len)
             (.position 0))
        left (.getLong bb)
        right (.getLong bb)
        values (map (fn [_] (.getInt bb))
                    (range (quot (- len 16) 4)))]
    [left right values]))

(defn values->bytes
  [values]
  (let [len (* 4 (count values))
        bb (doto (gen-byte-buffer len)
             (.limit len))]
    (doseq [v values]
      (.putInt bb v))
    (.position bb 0)
    (.array bb)))

(defn parse-body
  [res]
  (let [[left right values] (stream->values (:body res)
                                            (str->int (get (:headers res) "Content-Length")))]
    (assoc res :body (list left right values))))

(ns shtrom.config
  (:require [clojure.java.io :as io]))

(declare port data-dir)

(def ^:private default-config-filename "shtrom.config.clj")

(defn load-config
  ([]
     (load-config default-config-filename))
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
       (intern 'shtrom.config 'data-dir (:data-dir conf))
       (intern 'shtrom.config 'port (:port conf)))))

(ns shtrom.t-cache
  (:require [midje.sweet :refer :all]
            [clojure.java.io :as io]
            [shtrom.cache :as cache]
            [shtrom.config :as config]))

(defn delete-dir! [dir-or-file]
  (when (.isDirectory dir-or-file)
    (doseq [child (.listFiles dir-or-file)]
      (delete-dir! child)))
  (.delete dir-or-file))

(fact "shtrom.cache"
  (config/load-config "test2.shtrom.config.clj") => anything
  (delete-dir! (io/file config/data-dir)) => anything
  (cache/prepare-cache!) => anything)


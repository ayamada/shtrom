(ns shtrom.t-util
  (:require [midje.sweet :refer :all]
            [shtrom.t-data :as t-data]
            [shtrom.t-common :as t-common]
            [clojure.java.io :as io]
            [shtrom.util :as util]))

(defn- prepare-histfile! []
  (.mkdirs (io/file t-data/test-dir))
  (let [histfile (str t-data/test-dir "/dummy.hist")]
    (spit histfile
          (apply str
                 (interpose "\n"
                            (map pr-str
                                 [{:val 1}
                                  {:val 222}
                                  {:val 33}
                                  {:val 44}])))))
  (let [not-histfile (str t-data/test-dir "/dummy.txt")]
    (spit not-histfile "test")))

(defn- clean-histfile! []
  (util/delete-if-exists (str t-data/test-dir "/dummy.hist"))
  (util/delete-if-exists (str t-data/test-dir "/dummy.bist"))
  (util/delete-if-exists (str t-data/test-dir "/dummy.txt"))
  (util/delete-if-exists t-data/test-dir))

(with-state-changes [(before :facts (prepare-histfile!))
                     (after :facts (clean-histfile!))]
  (fact "hist->bist"
    (util/hist->bist (str t-data/test-dir "/dummy.txt")) => (throws Exception)
    (util/dir-hist->bist t-data/test-dir) => anything
    (.exists (io/file (str t-data/test-dir "/dummy.bist"))) => truthy))

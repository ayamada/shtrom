(ns shtrom.t-util
  (:require [midje.sweet :refer :all]
            [shtrom.t-fixture :as t-fixture]
            [shtrom.t-common :as t-common]
            [clojure.java.io :as io]
            [shtrom.util :as util]))

(defn- in-test-dir [filename]
  (str t-fixture/test-dir "/" filename))

(defn- prepare-histfile! []
  (.mkdirs (io/file t-fixture/test-dir))
  (let [histfile (in-test-dir "dummy.hist")]
    (spit histfile
          (apply str
                 (interpose "\n"
                            (map pr-str
                                 [{:val 1}
                                  {:val 222}
                                  {:val 33}
                                  {:val 44}])))))
  (let [not-histfile (in-test-dir "dummy.txt")]
    (spit not-histfile "test")))

(defn- clean-histfile! []
  (util/delete-if-exists (in-test-dir "dummy.hist"))
  (util/delete-if-exists (in-test-dir "dummy.bist"))
  (util/delete-if-exists (in-test-dir "dummy.txt"))
  (util/delete-if-exists t-fixture/test-dir))

(with-state-changes [(before :facts (prepare-histfile!))
                     (after :facts (clean-histfile!))]
  (fact "hist->bist"
    (util/hist->bist (in-test-dir "dummy.txt")) => (throws Exception)
    (util/dir-hist->bist t-fixture/test-dir) => anything
    (.exists (io/file (in-test-dir "dummy.bist"))) => truthy))

(ns shtrom.t-config
  (:require [midje.sweet :refer :all]
            [shtrom.config :as config]))

(fact "shtrom.config"
  (try
    (config/load-config)
    (catch Throwable e nil)) => anything
  (config/load-config "not-exist-file") => (throws Throwable))

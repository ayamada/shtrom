(defproject shtrom "0.1.0-SNAPSHOT"
  :description "A histogram data store that is specialized for short read coverage"
  :url "http://github.com/chrovis/shtrom"
  :license {:name "Apache License, Version 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.logging "0.3.0"]
                 [ring/ring-core "1.3.0"]
                 [compojure "1.1.8"]
                 [ring/ring-jetty-adapter "1.3.0"]]
  :plugins [[lein-ring "0.8.2"]
            [lein-midje "3.1.3"]]
  :profiles {:dev {:dependencies [[ring-mock "0.1.5"]
                                  [midje "1.6.3"]
                                  [javax.servlet/servlet-api "2.5"]]}}
  :ring {:handler shtrom.handler/app
         :init shtrom.handler/init
         :port 3001
         :war-exclusions [#".+?\.config\.clj"]}
  :jar-exclusions [#".+?\.config\.clj"]
  :main shtrom.handler
  :aot [shtrom.handler])

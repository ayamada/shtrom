(defproject shtrom "0.1.0-SNAPSHOT"
  :description "Fastest histogram database"
  :url "http://github.com/chrovis/shtrom"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.logging "0.2.6"]
                 [javax.servlet/servlet-api "2.5"]
                 [compojure "1.1.6"]]
  :plugins [[lein-ring "0.8.10"]
            [lein-midje "3.1.3"]]
  :profiles {:dev {:dependencies [[ring-mock "0.1.5"]
                                  [midje "1.6.0"]]}}
  :ring {:handler shtrom.core.handler/app
         :init shtrom.core.handler/init
         :port 3001
         :war-exclusions [#".+?\.config\.clj"]}
  :jar-exclusions [#".+?\.config\.clj"])

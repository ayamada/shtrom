(defproject shtrom "0.1.0-SNAPSHOT"
  :description "histogram database"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :repositories [["snapshots" {:url "https://nexus.xcoo.jp/content/repositories/snapshots"}]
                 ["releases" {:url "https://nexus.xcoo.jp/content/repositories/releases"}]]
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [compojure "1.1.5"]]
  :plugins [[lein-ring "0.8.5"]]
  :ring {:handler shtrom.core.handler/app
         :init shtrom.core.handler/init
         :port 3001
         :war-exclusions [#".+?\.config\.clj"]}
  :jar-exclusions [#".+?\.config\.clj"])

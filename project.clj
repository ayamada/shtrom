(defproject shtrom "0.1.0-SNAPSHOT"
  :description "A histogram data store that is specialized for short read coverage"
  :url "http://github.com/chrovis/shtrom"
  :license {:name "Apache License, Version 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.slf4j/slf4j-log4j12 "1.7.14"]
                 [log4j/log4j "1.2.17" :exclusions [javax.mail/mail
                                                    javax.jms/jms
                                                    com.sun.jmdk/jmxtools
                                                    com.sun.jmx/jmxri]]
                 [ring/ring-core "1.4.0"]
                 [compojure "1.4.0"]
                 [ring/ring-jetty-adapter "1.4.0"]]
  :jar-exclusions [#".+?\.config\.clj"
                   #"log4j\.properties"]
  :plugins [[lein-ring "0.9.7"]
            [lein-cloverage "1.0.6"]
            [lein-midje "3.2"]]
  :profiles {:dev {:dependencies [[ring-mock "0.1.5"]
                                  [midje "1.8.3"]
                                  [shtrom-client "0.1.0-SNAPSHOT"]
                                  [javax.servlet/servlet-api "2.5"]]}}
  :ring {:handler shtrom.handler/app
         :init shtrom.handler/init
         :port 3001
         :war-exclusions [#".+?\.config\.clj"
                          #"log4j\.properties"]}
  :main shtrom.handler
  :aot :all
  :jvm-opts ["-Dcom.sun.management.jmxremote"
             "-Dcom.sun.management.jmxremote.ssl=false"
             "-Dcom.sun.management.jmxremote.authenticate=false"
             "-Dcom.sun.management.jmxremote.port=43001"])

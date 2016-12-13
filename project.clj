(defproject shtrom "0.1.0-SNAPSHOT"
  :description "Coverage(or depth) data store in DNA sequencing"
  :url "https://github.com/chrovis/shtrom"
  :license {:name "Apache License, Version 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0.html"}
  :source-paths ["src/clj"]
  :java-source-paths ["src/java"]
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.slf4j/slf4j-log4j12 "1.7.16"]
                 [log4j/log4j "1.2.17" :exclusions [javax.mail/mail
                                                    javax.jms/jms
                                                    com.sun.jmdk/jmxtools
                                                    com.sun.jmx/jmxri]]
                 [ring/ring-core "1.4.0"]
                 [ring/ring-jetty-adapter "1.4.0"]
                 [ring/ring-defaults "0.1.5"]
                 [compojure "1.4.0"]
                 [cheshire "5.5.0"]]
  :jar-exclusions [#".+?\.config\.clj"
                   #"log4j\.properties"]
  :profiles {:dev {:plugins [[lein-ring "0.9.7"]]
                   :dependencies [[javax.servlet/servlet-api "2.5"]]}
             :jmx {:jvm-opts ["-Dcom.sun.management.jmxremote"
                              "-Dcom.sun.management.jmxremote.ssl=false"
                              "-Dcom.sun.management.jmxremote.authenticate=false"
                              "-Dcom.sun.management.jmxremote.port=43001"
                              "-Dcom.sun.management.jmxremote.rmi.port=43002"
                              "-Djava.rmi.server.hostname=localhost"]}
             :test {:resource-paths ["test-resources"]
                    :plugins [[lein-cloverage "1.0.6"]
                              [lein-midje "3.2"]]
                    :dependencies [[ring-mock "0.1.5"]
                                   [midje "1.8.3"]]}
             :uberjar {:aot :all
                       :main shtrom.handler
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}}
  :ring {:handler shtrom.handler/app
         :init shtrom.handler/init
         :destroy shtrom.handler/term
         :port 3001
         :war-exclusions [#".+?\.config\.clj"
                          #"log4j\.properties"]})

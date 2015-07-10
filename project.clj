(defproject user-http-api "0.1.0-SNAPSHOT"
  :description ""
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [turbovote.resource-config "0.1.4"]
                 [com.novemberain/langohr "3.2.0"]
                 [democracyworks/datomic-toolbox "1.0.0" :exclusions [com.datomic/datomic-pro]]
                 [prismatic/schema "0.4.3"]
                 [com.datomic/datomic-pro "0.9.5153" :exclusions [org.slf4j/slf4j-nop
                                                                  org.slf4j/slf4j-log4j12]]
                 [ch.qos.logback/logback-classic "1.1.3"]
                 [org.immutant/core "2.0.1"]
                 [democracyworks/kehaar "0.3.0"]]
  :plugins [[lein-immutant "2.0.0"]]
  :main ^:skip-aot user-http-api.core
  :target-path "target/%s"
  :repositories {"my.datomic.com" {:url "https://my.datomic.com/repo"
                                   :username [:gpg :env]
                                   :password [:gpg :env]}}
  :uberjar-name "user-http-api.jar"
  :profiles {:uberjar {:aot :all}
             :dev-common {:resource-paths ["dev-resources"]}
             :dev-overrides {}
             :dev [:dev-common :dev-overrides]})

(defproject user-http-api "0.1.0-SNAPSHOT"
  :description "HTTP API gateway for managing users"
  :url "https://github.com/democracyworks/user-http-api"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [turbovote.resource-config "0.2.0"]
                 [com.novemberain/langohr "3.2.0"]
                 [prismatic/schema "0.4.3"]
                 [ch.qos.logback/logback-classic "1.1.3"]
                 [org.immutant/core "2.0.2"]
                 [democracyworks/kehaar "0.4.0"]]
  :plugins [[lein-immutant "2.0.0"]]
  :main ^:skip-aot user-http-api.core
  :target-path "target/%s"
  :uberjar-name "user-http-api.jar"
  :profiles {:uberjar {:aot :all}
             :dev-common {:resource-paths ["dev-resources"]}
             :dev-overrides {}
             :dev [:dev-common :dev-overrides]})

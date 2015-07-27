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
                 [io.pedestal/pedestal.service "0.4.0"]
                 [io.pedestal/pedestal.service-tools "0.4.0"]
                 [democracyworks/pedestal-toolbox "0.6.1-SNAPSHOT"]

                 ;; this has to go before pedestal.immutant
                 ;; until this is fixed:
                 ;; https://github.com/pedestal/pedestal/issues/33
                 [org.immutant/web "2.0.2"]
                 [io.pedestal/pedestal.immutant "0.4.0"]
                 [org.immutant/core "2.0.2"]

                 [democracyworks/kehaar "0.4.0"]]
  :plugins [[lein-immutant "2.0.0"]]
  :main ^:skip-aot user-http-api.server
  :target-path "target/%s"
  :uberjar-name "user-http-api.jar"
  :profiles {:uberjar {:aot :all}
             :dev-common {:resource-paths ["dev-resources"]}
             :dev-overrides {}
             :dev [:dev-common :dev-overrides]
             :test {:dependencies [[clj-http "2.0.0"]]
                    :jvm-opts ["-Dlog-level=INFO"]}})

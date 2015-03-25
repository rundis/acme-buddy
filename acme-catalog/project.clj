(defproject acme-catalog "0.1.0-SNAPSHOT"
  :description "Acme Corp Product Catalog Service"
  :url "https://github.com/rundis/acme-buddy/acme-catalog"
  :min-lein-version "2.0.0"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/java.jdbc "0.3.6"]
                 [com.h2database/h2 "1.3.170"]
                 [hikari-cp "1.2.0"]
                 [compojure "1.3.2"]
                 [ring/ring-jetty-adapter "1.3.2"]
                 [ring/ring-json "0.3.1"]
                 [liberator "0.12.2"]
                 [buddy/buddy-auth "0.4.1"]]
  :ring {:handler acme-catalog.core/app
         :port 6003
         :init acme-catalog.core/bootstrap}
  :profiles {:dev {:plugins [[lein-ring "0.9.3"]]
                   :test-paths ^:replace []}
             :test {:dependencies [[midje "1.6.3"]]
                    :plugins [[lein-midje "3.1.3"]]
                    :test-paths ["test"]
                    :resource-paths ["test/resources"]}})

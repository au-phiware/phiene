(defproject phiene "0.1.0-SNAPSHOT"
  :description "A genetic algorithm framework."
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies
  [[org.clojure/clojure "1.7.0"]
   [org.clojure/core.async "0.2.371"]
   [org.clojure/math.numeric-tower  "0.0.4"]
   [net.mikera/core.matrix "0.43.0"]
   ;[com.datomic/datomic-free "0.9.5302"]
   [au.com.phiware/util "1.0-SNAPSHOT"]
   [au.com.phiware/math.bankers "0.1.0-SNAPSHOT"]
   [com.google.guava/guava "14.0"]
   [com.google.code.findbugs/jsr305 "1.3.9"]
   [org.slf4j/slf4j-api "1.7.5"]
   [com.codahale.metrics/metrics-core "3.0.0-BETA3"]
   [ch.qos.logback/logback-classic "1.0.12"]]
  :source-paths ["src/main/clj"]
  :java-source-paths ["src/main/java"]
  :prep-tasks [["compile" "au.com.phiware.phiene.core"] ["compile" "au.com.phiware.phiene.containers"] "javac" "compile"]
  :main au.com.phiware.phiene.core
  :target-path "target/%s"
  :jvm-opts  ["-Xms768m"  "-Xmx1024m"]
  :profiles {:uberjar {:aot :all}
             :provided {:dependencies [[org.apache.hadoop/hadoop-core "1.2.1"]]}}
  :datomic  {:schemas  ["resources"  ["schema.edn"]]})

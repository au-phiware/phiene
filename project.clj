(defproject phiene "0.1.0-SNAPSHOT"
  :description "A genetic algorithm framework."
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies
  [[org.clojure/clojure "1.9.0"]
   [org.clojure/core.async "0.4.490"]
   [org.clojure/math.numeric-tower  "0.0.4"]
   [org.clojure/tools.cli "0.4.1"]
   [net.mikera/core.matrix "0.62.0"]
   [iapetos "0.1.8"]
   ;[com.datomic/datomic-free "0.9.5302"]
   [au.com.phiware/util "1.0-SNAPSHOT"]
   [au.com.phiware/math.bankers "0.1.0-SNAPSHOT"]
   [com.google.guava/guava "27.0.1-jre"]
   [com.google.code.findbugs/jsr305 "3.0.2"]
   [org.slf4j/slf4j-api "1.7.25"]
   [io.dropwizard.metrics/metrics-core "4.0.5"]
   [ch.qos.logback/logback-classic "1.2.3"]]
  :source-paths ["src/main/clj"]
  :java-source-paths ["src/main/java"]
  :prep-tasks [["compile" "au.com.phiware.phiene.core"] ["compile" "au.com.phiware.phiene.containers"] "javac" "compile"]
  :main au.com.phiware.phiene.demo
  :target-path "target/%s"
  :jvm-opts  ["-Xms768m"  "-Xmx2048m"  "-Dclojure.core.async.pool-size=64"]

  :profiles {:uberjar {:aot :all}
             :provided {:dependencies [[org.apache.hadoop/hadoop-core "1.2.1"]]}}
  :datomic  {:schemas  ["resources"  ["schema.edn"]]})

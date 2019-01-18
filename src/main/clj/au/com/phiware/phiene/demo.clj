(ns au.com.phiware.phiene.demo
  (:require
    [clojure.core.async :refer [buffer chan close! onto-chan put!]]
    [clojure.string :as string]
    [clojure.tools.cli :refer [parse-opts]]
    [iapetos.core :as prometheus]
    [iapetos.standalone :as prom-standalone]
    [iapetos.registry :as prom-registry]
    [au.com.phiware.phiene.core :refer :all]
    [au.com.phiware.phiene.containers :refer [*parent-count*]]
    [au.com.phiware.phiene.calc :refer :all])
  (:import [java.util Arrays]
           [io.prometheus.client Collector GaugeMetricFamily]))

(defn- rand-bytes [size] (byte-array (repeatedly size #(-> 0xFF rand-int (- 0x80) byte))))

(defn- summary [x] (assoc (meta x) :parents (-> x meta :parents count)))

(defonce instrumented-buffers
  {"to" (buffer 400)
   "from" (buffer 200)
   "ticketed-meiosis" (buffer 10)
   "mutation" (buffer 10)
   "ticketed-fertilization" (buffer 10)
   "ticketed-tournament" (buffer 50)})

(defn initialize-buffer-instrumentation [registry]
  (prometheus/register
    registry
    (proxy [Collector] []
      (collect []
        (->> instrumented-buffers
             (reduce
               (fn [samples [^String label buf]]
                 (.addMetric samples
                             (Arrays/asList (into-array String [label]))
                             (count buf)))
               (GaugeMetricFamily.
                 "demo_buf_size" "buffer size (typically used for channels)"
                 (Arrays/asList (into-array String ["buf"]))))
             (vector)
             (into-array GaugeMetricFamily)
             Arrays/asList)))))

(defonce registry
  (-> (prometheus/collector-registry)
      (initialize-buffer-instrumentation)
      (prometheus/register
        (prometheus/histogram
          :demo/ind-ticket-count
          {:description "number of tickets held by an individual"
           :labels [:chan :winner]
           :buckets [0.0 1.0 2.0 3.0 4.0 5.0 6.0 7.0 8.0 9.0 10.0
                     11.0 12.0 13.0 14.0 15.0 16.0 17.0 18.0 19.0]})
        (prometheus/histogram
          :demo/ind-result
          {:description "the first number of an individual's stack after attempting to calculate the target"
           :labels [:chan :winner]
           :buckets [0.0 1.0 2.0 3.0 4.0 5.0 1e1 1e2 1e3]})
        (prometheus/histogram
          :demo/ind-stack-len
          {:description "length of an individual's stack after attempting to calculate the target"
           :labels [:chan :winner]
           :buckets [-1.0 0.0 1.0 2.0 3.0 4.0 5.0 6.0 7.0 8.0 9.0 10.0
                     20.0 30.0 40.0 50.0 60.0 70.0 80.0 90.0 100.0]})
        (prometheus/histogram
          :demo/ind-step
          {:description "number of calculations of an individual after attempting to calculate the target"
           :labels [:chan :winner]
           :buckets [ 1.0  2.0   3.0   4.0   5.0  10.0  15.0  20.0  25.0 30.0
                     60.0 90.0 120.0 150.0 200.0 250.0 300.0 350.0 400.0 500.0]})
        (prometheus/counter
          :demo/chan-put-total
          {:description "channel throughput as measured by its transducer"
           :labels [:chan :winner]}))))

(defn observe [metric chan winner value]
  (prometheus/observe registry metric {:chan chan :winner winner} value))

(def contestants (repeatedly 500 #(-> 60 rand-bytes (make {:ticket-count 4}))))

(def target 5)

(def to   (chan (instrumented-buffers "to")))
(def from (chan (instrumented-buffers "from")
                (map
                  (fn [v]
                    (let [ch-name "population"
                          result (calculate target (seq v))
                          attempt (->> result :stack first)
                          winner (= target attempt)]
                      (prometheus/inc registry :demo/chan-put-total {:chan chan :winner winner})
                      (observe :demo/ind-ticket-count ch-name winner
                               (-> v meta :ticket-count))
                      (observe :demo/ind-stack-len ch-name winner
                               (if attempt
                                 (count (:stack result))
                                 -1.0))
                      (observe :demo/ind-step ch-name winner
                               (:step result))
                      (when attempt
                        (observe :demo/ind-result ch-name winner attempt))
                      (when winner
                        (prn
                          (assoc
                            (merge
                              result
                              (summary v))
                            :genome-length (size v)))))
                    v))))

(defn -main [& args]
  (let [{:keys [options arguments errors summary]}
        (parse-opts args
                    [["-p" "--port PORT" "Port number"
                      :default 50001
                      :parse-fn #(Integer/parseInt %)
                      :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]

                     ["-h" "--help"]])
        ]
    (cond
      (:help options) (do (println summary) (System/exit 0))
      errors (do (println (string/join \newline errors)) (System/exit 1))
      :else
      (with-open [httpd (prom-standalone/metrics-server registry (select-keys options [:port]))]
        (println (str "Serving metrics at http://localhost:" (:port options) "/metrics"))
        (println "Press Enter to continue.")
        (read-line)

        (onto-chan from contestants false)

        (binding [*parent-count* 3
                  *ex-handler* #(let [name (:name (meta %))]
                                  (fn [t] (printf "%s: %s\n" name t)))
                  *interceptor* #(let [counter (registry :demo/chan-put-total {:chan (:name (meta %)) :winner false})]
                                   (comp % (map (fn [v] (prometheus/inc counter) v))))]
          (evolve to 6 from
                  (ticketed-meiosis)
                  (instrumented-buffers "ticketed-meiosis")
                  (mutation)
                  (instrumented-buffers "mutation")
                  (ticketed-fertilization)
                  (instrumented-buffers "ticketed-fertilization")
                  (ticketed-tournament (compete target))
                  (instrumented-buffers "ticketed-tournament")))

        (read-line)
        (close! from)
        (println "Press Enter to exit.")
        (read-line)))))

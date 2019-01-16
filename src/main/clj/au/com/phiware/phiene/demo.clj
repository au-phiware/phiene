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
                 "demo_buffer_size" "a channel's buffer size"
                 (Arrays/asList (into-array String ["name"]))))
             (vector)
             (into-array GaugeMetricFamily)
             Arrays/asList)))))

(defonce registry
  (-> (prometheus/collector-registry)
      (initialize-buffer-instrumentation)
      (prometheus/register
        (prometheus/counter :demo/tx_total {:description "transformations"
                                            :labels [:name]}))))

(def contestants (repeatedly 100 #(-> 60 rand-bytes (make {:ticket-count 4}))))

(def target 5)

(def winners
  (chan 100
        (map
          (fn [v]
            (prn
              (assoc
                (merge
                  (calculate target
                             (seq v))
                  (summary v))
                :genome-length (size v)))
            v))))

(def to   (chan (instrumented-buffers "to")))
(def from (chan (instrumented-buffers "from")
                (map
                  (fn [v]
                    (when (= target (->> v seq (calculate target) :stack first))
                      (prn
                        (assoc
                          (merge
                            (calculate target
                                       (seq v))
                            (summary v))
                          :genome-length (size v))))
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
                  *interceptor* #(let [counter (registry :demo/tx_total (select-keys (meta %) [:name]))]
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
        (close! from)))))

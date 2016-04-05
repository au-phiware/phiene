(ns au.com.phiware.phiene.core
  (:require
    [clojure.core.async :as async :refer [chan close! >!! >! <! go go-loop]]
    [au.com.phiware.phiene.containers :refer :all]))

(defprotocol GenomeContainer
  (alloc [g] [g size] [g size m])
  (dealloc [g])
  (get-at [g i])
  (set-at! [g i n])
  (size [g]))

(def ^:dynamic *mutation-frequency* 0.00001)
(def ^:dynamic *mutation-bit-rate* 0.0001)
(def ^:dynamic *crossover-frequency* 0.001)

(defn- to-byte [x] (byte (if (bit-test x 7) (- x 0x100) x)))

(comment basically an unordered version of async/pipeline,
         see "http://dev.clojure.org/jira/browse/ASYNC-150")
(defn pipeline
  "Takes elements from the from channel and supplies them to the to
  channel, subject to the transducer xf, with parallelism n. Because
  it is parallel, the transducer will be applied independently to each
  element, not across elements, and may produce zero or more outputs
  per input.  Outputs may be returned unordered relative to the
  inputs. By default, the to channel will be closed when the from
  channel closes, but can be determined by the close?  parameter. Will
  stop consuming the from channel if the to channel closes. Note this
  should be used for computational parallelism."
  ([n to xf from] (pipeline n to xf from true))
  ([n to xf from close?] (pipeline n to xf from close? nil))
  ([n to xf from close? ex-handler]
   (assert (pos? n))
   (let [ex-handler (or ex-handler (fn [ex]
                                     (-> (Thread/currentThread)
                                         .getUncaughtExceptionHandler
                                         (.uncaughtException (Thread/currentThread) ex))
                                     nil))]
     (go
       (<! (async/merge
             (repeatedly n
                         #(let [res (chan 1 xf ex-handler)]
                            (go-loop []
                                     (if-let [v (<! from)]
                                       (do
                                         (>!! res v)
                                         (recur))
                                       (close! res)))
                            (go-loop []
                                     (let [v (<! res)]
                                       (when (and (not (nil? v)) (>! to v))
                                         (recur))))))))
       (when close? (close! to))))))

(defn evolve
  "Takes genetic containers from the from channel and continously evolves them
  subject to the transducers in the xform-clauses, with parallelism n. Where
  the xform-clauses is the transducer itself or optionally preceeded by a
  buffer (or size of a fixed buffer), to which the tranducer will be pipelined
  into. If not buffer is specified the transducer will pipelined into a fixed
  sized buffer equal to n.
  Once a container has been pipelined through the tranducers it will be put on
  the from channel again; to monitor the individuals in the population consider
  using a from channel that has a transducer.
  The evolution will cease when the from channel closes; the surviving
  containers are then placed on the to channel, and the to channel is closed."
  [to n from & xform-clauses]
  (let [popn (loop [from from
                    [buf xform & xforms] xform-clauses]
               (if (fn? buf)
                 (recur from (concat [n buf xform] xforms))
                 (if xform
                   (let [to (chan buf)]
                     (pipeline n to xform from)
                     (recur to xforms))
                   from)))]
    (go-loop []
             (if-let [ind (<! popn)]
               (do (when-not (>! from ind)
                     (>! to ind))
                   (recur))
               (close! to)))))

(defn tournament
  ([compete]
   (let [n *parent-count*]
     (comp
       (partition-all n)
       (map (comp first compete))))))

(defn ticketed-tournament
  ([compete] (ticketed-tournament compete 1))
  ([compete cost]
   (let [n *parent-count*]
     (comp
       (map #(with-meta %
                   (assoc (meta %)
                          :ticket-count (-> % meta :ticket-count (- cost)))))
       (partition-all n)
       (map compete)
       (mapcat #(cons (with-meta (first %)
                            (assoc (-> % first meta)
                                   :ticket-count (+ (* cost n) (-> % first meta :ticket-count))))
                      (rest %)))
       (filter #(not (when (-> % meta :ticket-count (<= 0)) (dealloc %) true)))))))

(defn- crossover
  ([n freq ind m]
   (-> ind
     (#(let [cnt (-> % size (/ n))]
         (loop [gamete (alloc % cnt)
                i (rand-int n)
                j 0]
           (if (< j cnt)
             (recur (set-at! gamete j (get-at % (+ i (* n j))))
                    (if (< (rand) freq) (rand-int n) i)
                    (inc j))
             gamete))))
     (with-meta m)))
  ([n freq ind] (crossover n freq ind (meta ind))))

(defn meiosis
  ([] (let [n *parent-count*
            f *crossover-frequency*] (map (partial crossover n f)))))

(comment
  ;; Regardless of the *crossover-frequency* the result should be about 500
  binding [*crossover-frequency* 0.01]
  (let [population (repeatedly 100 #(*make-container* (byte-array (flatten (repeat 10 [0 1])))))]
    (apply + (flatten (map (comp seq deref) (transduce (meiosis) conj population))))))

(defn ticketed-meiosis
  ([] (let [n *parent-count*
            f *crossover-frequency*]
        (mapcat (fn [ind]
                  (repeatedly (max (:ticket-count (meta ind)) 1)
                              #(crossover n f ind (assoc (meta ind) :ticket-count 1))))))))

(defn mutation
  ([] (let [freq *mutation-frequency*
            rate *mutation-bit-rate*]
        (map (fn [ind]
               (let [limit (* freq (size ind))
                     bits (* 8 (size ind))
                     step (/ freq rate bits)]
                 (loop [o (rand)]
                   (when (<= o limit)
                     (let [f (rand-int bits)
                           b (bit-and 7 f)
                           f (bit-shift-right f 3)
                           b (bit-shift-left 1 b)]
                       (set-at! ind f (bit-xor (get-at ind f) (to-byte b))))
                     (recur (+ o step)))))
               ind)))))

(comment
  into (sorted-map)
  (let
    [genome-length 1000
     pop-size 1000]
    (binding [;; increasing *mutation-frequency* increases the number of individuals
              ;; selected for mutation. If this value is high enough every individual
              ;; is guaranteed to be mutated, the serverity depends on the number of
              ;; bits in the individual's genome and the next dynamic variable.
              *mutation-frequency* 0.001
              ;; increasing *mutation-bit-rate* increases the number of bits that are mutated
              ;; within an individual selected for mutation. The number of bits mutated per
              ;; individual is normally distributed, increases also increases the spread.
              *mutation-bit-rate* 0.001]
      (frequencies
        (transduce
          (comp (mutation)
                (map (fn [g] (apply + (flatten (map #(byte-test %) (deref g)))))))
          conj
          (repeatedly pop-size #(*make-container* (byte-array (repeat genome-length 0)))))))))

(defn fertilization
  ([] (let [n *parent-count*]
        (comp
          (partition-all n)
          (map #(with-meta
                  (let [cnt (-> % first size)]
                    (loop [genome (alloc (first %) (* cnt n))
                           i 0]
                     (if (< i cnt)
                       (recur (loop
                                [genome genome
                                 j 0]
                                (if (< j n)
                                  (recur (set-at! genome (+ (* i n) j) (get-at (nth % j) i))
                                         (inc j))
                                  genome))
                              (inc i))
                       genome)))
                  :parents %
                  :generation (->> %
                                (map (comp :generation meta))
                                (filter (comp not nil?))
                                (apply max 0)
                                inc)))))))

(comment
  let [population  (map #(*make-container* (byte-array (repeat 10 %))) (range 10 100))]
  (transduce (fertilization) conj population))

(defn ticketed-fertilization
  ([] (let [n *parent-count*]
        (comp
          (partition-all n)
          (filter #(-> % count (= n)))
          (map #(with-meta
                  (let [cnt (-> % first size)]
                    (loop [genome (alloc (first %) (* cnt n))
                           i 0]
                     (if (< i cnt)
                       (recur (loop
                                [genome genome
                                 j 0]
                                (if (< j n)
                                  (recur (set-at! genome (+ (* i n) j) (get-at (nth % j) i))
                                         (inc j))
                                  genome))
                              (inc i))
                       genome)))
                  {:parents %
                   :generation (->>
                                 % (map (comp :generation meta))
                                 (filter (comp not nil?))
                                 (apply max 0)
                                 inc)
                   :ticket-count (apply + (map (comp :ticket-count meta) %))}))))))

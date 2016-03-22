(ns au.com.phiware.phiene.core
  (:require
    [clojure.core.matrix :refer :all]
    [clojure.test :refer :all]
    [clojure.core.async :as async :refer [chan close! >!! >! put! thread <!! <! go go-loop onto-chan]]
    [datomic.api :as d]))

(def ^:dynamic *mutation-frequency* 0.00001)
(def ^:dynamic *mutation-bit-rate* 0.0001)
(def ^:dynamic *crossover-frequency* 0.001)
(def ^:dynamic *parent-count* 2)
(def ^:dynamic *make-container*
  (fn
    ([b] (ref b))
    ([b m] (ref b :meta m))
    ([b k & m] (ref b :meta (apply hash-map k m)))))
(def ^:dynamic *conj-container*
  (fn
    ([] (*make-container* []))
    ([ind] ind)
    ([ind x] (*make-container* (conj (deref ind) x) (meta ind)))))

(defn- tap
  ([xf fmt f] (comp (map (fn [x] (printf fmt (f x)) x)) xf))
  ([xf fmt]   (tap xf fmt identity))
  ([xf]       (tap xf "%s%n"))
  ([]         (tap (map identity))))

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

(defn evolve1
  [xform repeats popn]
  (loop [i repeats
         p popn]
    (if (zero? i)
      p
      (recur (dec i) (transduce xform conj p)))))

(defn byte-test [b] (map #(if (bit-test b %) 1 0) (range 8)))
(defn to-byte [x] (byte (if (bit-test x 7) (- x 0x100) x)))
(defn byte-cardinality [b] (apply + (byte-test b)))

(comment defn transducer [xf]
         (fn
           ([] ...)
           ([env] ...)
           ([env ind] ...)))

(def G [0xD2, 0x55, 0x99, 0xE1 0xD2, 0x55, 0x99, 0xE1])

(defn encode-byte
  ([n b] (->>
           b
           byte-test
           (mul G)
           (partition 4)
           (mapcat #(repeat n (to-byte (apply bit-xor %))))
           reverse)))

(defn encode
  ([n] (fn [c] (transduce (mapcat #(encode-byte n %)) conj c)))
  ([n c] ((encode n) c)))

(comment into-array Byte/TYPE (encode *parent-count* c))

(def H [0x1, 0xF, 0x7, 0xB, 0x3, 0xD, 0x5, 0x9])
(def revH [1 1, 0 0x10, 0 0x40, 0 4, 0 0x80, 0 8, 0 0x20, 0 2])

(defn decode-nibble
  [b] (second
        (first
          (into
            (sorted-map) ;; or'ed decoded nibbles, keyed by error-rate
            #(fn
               ([r] (% r))
               ([smap pair] (% smap [(pair 0) (bit-or (pair 1) (smap (pair 0) 0))])))
            (map
              #(let [synd (->> % byte-test (mul H) (apply bit-xor))
                     mask (revH synd)
                     errr (if (zero? synd) 0 (if (zero? mask) 2 1))
                     corr (bit-xor % mask)]
                 [errr (bit-shift-right
                         (bit-or
                           (bit-shift-right (bit-and corr 32) 1)
                           (bit-and corr 14))
                         1)])
              b)))))

(defn decode
  ([n] (fn [ind]
        (transduce
          (comp
            (partition-all n)
            (map decode-nibble)
            (partition-all 2)
            (map (fn [[high low]]
                   (if (= low nil) (recur [0 high])
                     (bit-or (bit-shift-left high 4) low)))))
          conj
          ind)))
  ([n ind] ((decode n) ind)))

(comment map (fn [n] (map #(-> % (bit-xor (second (encode 1 [n]))) cardinality) (filter #(= (decode 1 [0 %]) [n]) (map to-byte (range 0x100))))) (range 0x10))

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
       (map #(*make-container* (deref %) (assoc (meta %) :ticket-count (-> % meta :ticket-count (- cost)))))
       (partition-all n)
       (map compete)
       (mapcat #(cons (*make-container* (-> % first deref) (assoc (-> % first meta) :ticket-count (+ (* cost n) (-> % first meta :ticket-count)))) (rest %)))
       (filter #(-> % meta :ticket-count (> 0)))))))

(defn- crossover
  ([n freq ind m]
   (->
     (map #(nth %1 (mod %2 (count %1)))
          (partition-all n (-> ind deref seq))
          (iterate (fn [b]
                     (if (< (rand) freq)
                       (mod (+ 1 b (rand-int  (dec n))) n)
                       b))
                   (rand-int n)))
     byte-array
     (*make-container* m)))
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
                  (repeatedly (or (:ticket-count (meta ind)) 1)
                              #(crossover n f ind (assoc (meta ind) :ticket-count 1))))))))

(defn mutation
  ([] (let [freq *mutation-frequency*
            rate *mutation-bit-rate*]
        (map (fn [ind]
               (let [genome (deref ind)
                     limit (* freq (alength genome))
                     bits (* 8 (alength genome))
                     step (/ freq rate bits)]
                 (loop [o (rand)]
                   (when (<= o limit)
                     (let [f (rand-int bits)
                           b (bit-and 7 f)
                           f (bit-shift-right f 3)
                           b (bit-shift-left 1 b)]
                       (aset-byte genome f (bit-xor (aget genome f) (to-byte b))))
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
          (map #(*make-container*
                  (byte-array (apply mapcat (fn [& a] a) (map deref %)))
                  :parents %
                  :generation (->>
                                % (map (comp :generation meta))
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
          (map #(*make-container*
                  (byte-array (apply mapcat (fn [& a] a) (map deref %)))
                  :parents %
                  :generation (->>
                                % (map (comp :generation meta))
                                (filter (comp not nil?))
                                (apply max 0)
                                inc)
                  :ticket-count (apply + (map (comp :ticket-count meta) %))))))))

(defn- rand-bytes [size] (byte-array (repeatedly size #(-> 0xFF rand-int (- 0x80) byte))))

(deftest encode-rand
  (loop [n 9]
    (let [b (decode n (rand-bytes 2520))]
      (when (> n 0) (is (= b (decode n (encode n b))))))))

(defn- test-decode
  ([] (test-decode 1))
  ([n] (test-decode n identity))
  ([n f]
   (is (= (decode n (map f (encode n (range 0x100)))) (range 0x100)))))

(deftest decode-total
  (loop [n 10]
    (when (> n 0) (test-decode n) (recur (dec n)))))
(deftest decode-with-error
  (loop [i 0]
    (when (< i 8) (test-decode 1 #(bit-flip % i)) (recur (inc i)))))
(deftest combo-decode-with-error
  (loop [i 0]
    (when (< i 8)
      (loop [n 10]
        (when (> n 1)
          (test-decode n #(bit-flip % i))
          (recur (dec n))))
      (recur (inc i)))))

(deftest dominance
  (is (= (decode 2 [0 0 -46 85, ;; both dominate
                    0 0 -46 69, ;; 1 dominate
                    0 0 -46 68, ;; 1 dominate
                    0 0  82 85, ;; 2 dominate
                    0 0  82 69, ;; both major recessive
                    0 0  82 68, ;; 1 major recessive
                    0 0  83 85, ;; 2 dominate
                    0 0  83 69, ;; 2 major recessive
                    0 0  83 68]);; both (minor) recessive
         [3 1 1 2 3 1 2 2 3])))

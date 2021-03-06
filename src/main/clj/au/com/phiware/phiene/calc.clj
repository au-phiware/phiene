(ns au.com.phiware.phiene.calc
  (:require
    [clojure.math.numeric-tower :refer [abs]]
    [au.com.phiware.phiene.core :refer :all]
    [au.com.phiware.phiene.containers :as c :refer [decode]])
  (:import
    (clojure.lang IDeref IObj RT Seqable)
    (java.nio ByteBuffer)
    (au.com.phiware.phiene ByteBufferGenomeContainer)))

(def ^:private ^:const ByteArray (RT/classForName "[B"))
(defn make
  ([n] (make n {}))
  ([n m] (proxy [ByteBufferGenomeContainer Seqable]
           [(condp instance? n
              ByteBufferGenomeContainer (.buffer n)
              ByteBuffer n
              ByteArray  (ByteBuffer/wrap n)
              Number     (ByteBuffer/allocate n))
            m]
           (alloc
             ([n] (make n (meta this)))
             ([n m] (make n m)))
           (withMeta [m] (make this m))
           (seq []
             (let [buffer (.rewind @this)]
               (if (.hasRemaining buffer)
                 (take-while (fn [x] (.hasRemaining buffer))
                             (repeatedly #(.get buffer)))
                 (list)))))))

(defmacro pull [argn stack & body]
  (list 'if (list '>= (list 'count stack) argn)
        (list 'cons
              (concat (list 'apply) body (list (list 'take argn stack)))
              (list 'drop argn stack))
        stack))

(def ops
  [(fn [stack] [])
   (fn [stack] (pull 2 stack min))
   (fn [stack] (pull 2 stack max))
   (fn [stack] (pull 2 stack bit-or))
   (fn [stack] (pull 2 stack bit-and))
   (fn [stack] (pull 2 stack (comp bit-not bit-and)))
   (fn [stack] (pull 2 stack bit-xor))
   (fn [stack] (if (> (count stack) 0) (cons (first stack) stack) stack))
   (fn [stack] (if (and (>= (count stack) 2) (not (== (second stack) 0))) (cons (apply mod (take 2 stack)) (drop 2 stack)) stack))
   (fn [stack] (pull 1 stack bit-not))
   (fn [stack] (if (>= (count stack) 1) (cons (.longValue (- 0N (bigint (first stack)))) (rest stack)) stack))
   (fn [stack] (if (>= (count stack) 2) (cons (.longValue (- (first stack) (bigint (second stack)))) (drop 2 stack)) stack))
   (fn [stack] (if (>= (count stack) 2) (cons (.longValue (* (first stack) (bigint (second stack)))) (drop 2 stack)) stack))
   (fn [stack] (if (>= (count stack) 2) (cons (.longValue (+ (first stack) (bigint (second stack)))) (drop 2 stack)) stack))
   (fn [stack] (cons 1 stack))
   (fn [stack] (cons 0 stack))])

(def ^:dynamic *step-limit* 500)

(defn calculate
  [target instructions]
  (loop [i {:stack [], :step 0}]
    (if (or (and (> (count (:stack i)) 0) (== target (first (:stack i))))
            (>= (:step i) *step-limit*))
      i
      (recur {:stack (let
                       [n (mod (:step i) (count instructions))
                        op (ops (bit-and 0xF (nth instructions n)))]
                       (op (:stack i)))
              :step (inc (:step i))}))))

(defn compete
  ([target]
   (fn [contestants]
     (sort-by
       (comp
         (juxt :step
               (comp #(if % (abs (- target %)) Double/POSITIVE_INFINITY) first :stack)
               (comp count :stack))
         (partial calculate target)
         seq)
       contestants)))
  ([target contestants] ((compete target) contestants)))

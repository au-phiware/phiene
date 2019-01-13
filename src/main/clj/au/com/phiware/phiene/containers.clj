(ns au.com.phiware.phiene.containers
  (:gen-class
    :name au.com.phiware.phiene.Containers
    :methods [^:static [decode [int Object] Object]
              ^:static [encode [int Object] Object]])
  (:import (java.nio ByteBuffer)
           (clojure.lang RT))
  (:require
    [au.com.phiware.math.bankers :refer [to from]]
    [clojure.core.matrix :refer :all]
    [clojure.test :refer :all]))

(def ^:dynamic *parent-count* 2)

(defmulti decode #(type %2))

(defn -decode [n ind] (decode n ind))

(defn- byte-test [b] (map #(if (bit-test b %) 1 0) (range 8)))
(defn- to-byte [x] (byte (if (bit-test x 7) (- x 0x100) x)))
(defn- byte-cardinality [b] (apply + (byte-test b)))

(def G [0xD2, 0x55, 0x99, 0xE1 0xD2, 0x55, 0x99, 0xE1])

(defn- encode-byte
  ([n b] (->>
           b
           to
           byte-test
           (mul G)
           (partition 4)
           (mapcat #(repeat n (to-byte (apply bit-xor %))))
           reverse)))

(defn encode
  ([n] (fn [c] (transduce (mapcat #(encode-byte n %)) conj c)))
  ([n c] ((encode n) c)))

(defn -encode [n c] (encode n c))

(comment into-array Byte/TYPE (encode *parent-count* c))

(def H [0x1, 0xF, 0x7, 0xB, 0x3, 0xD, 0x5, 0x9])
(def revH [1 1, 0 0x10, 0 0x40, 0 4, 0 0x80, 0 8, 0 0x20, 0 2])

(defn- decode-nibble
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

(defn- conj-nibble [[high low]]
  (if (= low nil) (recur [0 high])
    (from (bit-or (bit-shift-left high 4) low))))

(defmethod decode :default
  [n ind]
  (transduce
    (comp
      (partition-all n)
      (map decode-nibble)
      (partition-all 2)
      (map conj-nibble))
    conj
    ind))

(def ^:private ^:const ByteArray (RT/classForName "[B"))
(defmethod decode ByteArray [n ^"[B" ind]
  (byte-array (decode n (seq ind))))

(defmethod decode ByteBuffer [n ^ByteBuffer ind]
  (let [ind (.. ind duplicate rewind)]
    (loop [buf (ByteBuffer/allocate (-> ind .limit (/ n 2) int))]
      (if (.hasRemaining buf)
        (recur (.put buf (to-byte
                           (conj-nibble
                             (repeatedly
                               2 #(decode-nibble
                                    (repeatedly
                                      n (fn [] (.get ind)))))))))
        (.rewind buf)))))

(comment map (fn [n] (map #(-> % (bit-xor (second (encode 1 [n]))) cardinality) (filter #(= (decode 1 [0 %]) [n]) (map to-byte (range 0x100))))) (range 0x10))

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

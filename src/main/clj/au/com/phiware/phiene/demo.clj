(ns au.com.phiware.phiene.demo
  (:require
    [clojure.core.async :refer [chan close! onto-chan put!]]
    [au.com.phiware.phiene.core :refer :all]
    [au.com.phiware.phiene.containers :refer [*parent-count*]]
    [au.com.phiware.phiene.calc :refer :all]))

(defn- rand-bytes [size] (byte-array (repeatedly size #(-> 0xFF rand-int (- 0x80) byte))))

(defn- summary [x] (assoc (meta x) :parents (-> x meta :parents count)))

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

(def to   (chan 400))
(def from (chan 200
                (map
                  (fn [v]
                    (when (= target (->> v seq (calculate target) :stack first))
                      (put! winners v))
                    v))))

(onto-chan from contestants false)

(binding [*parent-count* 3]
  (evolve to 6 from
          10 (ticketed-meiosis)
          10 (mutation)
          10 (ticketed-fertilization)
          10 (ticketed-tournament (compete target))))

(comment close! from)

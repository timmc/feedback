(ns org.timmc.pipeline.test.core
  (:require [org.timmc.pipeline.core :as pl] :reload)
  (:use [clojure.test])
  (:use [clojure.contrib.math :only (gcd lcm)]))


(defn A
  [f1 e]
  (if f1 (+ 3 e) (* 2 e)))

(defn B
  [f2]
  f2)

(defn C
  [a b]
  (gcd a b))

(defn D
  [a b]
  (lcm a b))

(defn E
  [d]
  (quot d 3))

(defn F
  [c]
  (+ c 7))

(defn F1
  [f]
  (zero? (mod f 2)))

(pl/create
 [:a :b :c :e]
 [:e]
 :A A [:f1 :e] :a
 :B B [:f2] :b
 :C C [:a :b] :c
 :D D [:a :b] :d
 :E E [:d] :e
 :F F [:c] {:f1 F1 :f2 identity})

(pl/initialize :a 0 :b 1)

#_
(pl/create
 [[]]
 [[:next #(if (zero? %1) %2 %3) [:parity :half :triplus] [:n] :initial]
  [:done #(= 1 %) [:n] [] :halt]
  [:decoder #(mod % 2) [:n] [:parity]]
  [:down #(quot % 2) [:n] [:half]]
  [:up #(inc (* 3 %)) [:n] [:triplus]]])


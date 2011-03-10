(ns org.timmc.pipeline.test.core
  (:use [org.timmc.pipeline.core] :reload)
  (:use [clojure.test])
  (:use [clojure.contrib.math :only (gcd lcm)]))

;;;; Evil stuff to allow testing of private defns

;; Thanks to Stuart Sierra
;; <https://groups.google.com/group/clojure/msg/66f15396411e49e9>
(doseq [[symbol var] (ns-interns (the-ns 'org.timmc.pipeline.core))]
  (when (:private (meta var))
    (intern *ns* symbol var))) 

;;;; Functions to ues as logic blocks

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

(def full
  (create
   [:A A [:f1 :e] :a]
   [:B B [:f2] :b]
   [:C C [:a :b] :c]
   [:D D [:a :b] :d]
   [:E E [:d] :e]
   [:F F [:c] {:f1 F1 :f2 identity}]))

;;;; Tests

(defmacro succeed
  "Assert that no exception is thrown. (A little silly.)"
  [& exprs]
  `(is (do ~@exprs true)))

(deftest creation
  (is (not (.initialized? (create))))
  (let [single (add (create) :A A [:f1 :e] :a)]
    (is (= (count (.blocks single)) 1))
    (is (not (.initialized? single)))))

(deftest additions
  (is (= (-> (create [:A A [:b] :pass]) (.blocks) :A :outputs)
         {:pass identity}))
  (is (= (-> (create [:A A [:b] {}]) (.blocks) :A :outputs)
         {})))

(deftest initialization
  (let [with-reg (merge-registers full {:foo 4 :bar 5})
        more-reg (merge-registers with-reg {:foo 7})]
    (is (= (.registers more-reg) {:foo 7 :bar 5}))
    (is (= (find-input-block-name more-reg :foo) nil))
    (is (= (find-input-block-name more-reg :b) :B)))
  #_
  (let [full-init (initialize full {:a 5})]
    (is (= (peek-register full-init :a) 5))
    (is (= (peek-wire full-init :b) 5))
    (is (= (find-input-block-name full-init :a) nil))
    (is (= (find-input-block-name full-init :b) :B))))

#_
(create
 [:next #(if (zero? %1) %2 %3) [:parity :half :triplus] :n]
 [:done #(= 1 %) [:n] {}]
 [:decoder #(mod % 2) [:n] :parity]
 [:down #(quot % 2) [:n] :half]
 [:up #(inc (* 3 %)) [:n] :triplus])


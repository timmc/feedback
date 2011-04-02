(ns org.timmc.test.feedback
  (:use [org.timmc.feedback] :reload)
  (:use [clojure.test])
  (:use [clojure.contrib.math :only (gcd lcm)]))

;;;; Evil stuff to allow testing of private defns

;; Thanks to Stuart Sierra
;; <https://groups.google.com/group/clojure/msg/66f15396411e49e9>
(doseq [[symbol var] (ns-interns (the-ns 'org.timmc.feedback))]
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
  (let [outs (-> (create [:A A [:b] :pass]) (.blocks) :A :outputs)]
    (is (= (count outs) 1))
    (is (= (key (first outs)) :pass))
    (is (= (-> (val (first outs)) meta :wraps) identity)))
  (is (= (-> (create [:A A [:b] {}]) (.blocks) :A :outputs)
         {})))

(deftest accessors
  (let [with-reg (merge-registers full {:foo 4 :bar 5})
        more-reg (merge-registers with-reg {:foo 7})]
    (is (= (.registers more-reg) {:foo 7 :bar 5}))
    (is (= (find-input-block-name more-reg :foo) nil))
    (is (= (find-input-block-name more-reg :b) :B))
    (is (thrown? IllegalStateException (read-register more-reg :foo)))
    (is (= (read-register (assoc-in more-reg [:initialized?] true) :foo) 7))))

(deftest topology
  (let [with-reg (merge-registers full {:a 27 :b 13})]
    (is (= (vec (block-depends with-reg :C)) []))
    (is (= (vec (block-depends with-reg :A)) [[:A :F] [:A :E]]))
    (is (= (vec (sorted-block-names with-reg)) [:D :E :C :F :A :B]))))

(defn collatz
  [n]
  (init
   (create
    [:next #(if (zero? %1) %2 %3) [:parity :half :triplus] :n]
    [:done #(= 1 %) [:n] :halt] ; dead-end node is important test-case
    [:decoder #(mod % 2) [:n] :parity]
    [:down #(quot % 2) [:n] :half]
    [:up #(inc (* 3 %)) [:n] :triplus])
   {:n n}))

(deftest collatz-count
  (let [c27 (collatz 27)]
    (is (= (block-depends c27 :done) []))
    (is (= (vec (sorted-block-names c27))
           [:decoder :done :down :up :next]))
    (let [steps (iterate step c27)]
      (is (= (read-register (nth steps 0) :n) 27))
      (is (= (read-wire (nth steps 0) :halt) false))
      (is (= (read-register (nth steps 1) :n) 82))
      (is (= (read-wire (nth steps 1) :halt) false))
      (is (= (read-register (nth steps 2) :n) 41))
      (is (= (read-wire (nth steps 2) :halt) false))
      (is (= (first (keep-indexed (fn [i el] (if (read-wire el :halt) i nil))
                                  steps))
             111)))))

(deftest de-init
  (let [cmod (add (collatz 27) :foo inc [] :bar)]
    (is (thrown? IllegalStateException (step cmod)))))

(deftest error-annotation
  (let [throw-nullary #(throw (Exception. "badmain"))
        bad-main (create [:x throw-nullary [] :y])
        thrown (try (init bad-main {})
                    false
                    (catch Exception e
                      e))]
    (is (thrown-with-msg? Exception #"core output for block :x" (throw thrown)))
    (is (thrown-with-msg? Exception #"badmain" (throw (.getCause thrown)))))
  (let [throw-unary (fn [_] (throw (Exception. "badout")))
        bad-out (create [:x inc [:y] {:z throw-unary}])      
        thrown (try (init bad-out {:y 4})
                    false
                    (catch Exception e
                      e))]
    (is (thrown-with-msg? Exception #":z output for block :x" (throw thrown)))
    (is (thrown-with-msg? Exception #"badout" (throw (.getCause thrown))))))

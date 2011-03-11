(defproject org.timmc/feedback "0.1.0-SNAPSHOT"
  :description "Logic pipeline manager."
  :author "Tim McCormack"
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [org.satta/loom "0.1.0-SNAPSHOT"]]
  :dev-dependencies [[lein-clojars "0.6.0"]]
  :warn-on-reflection true
  :jar-name "feedback.jar"
  :uberjar-name "feedback-standalone.jar")

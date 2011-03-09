(defproject org.timmc.pipeline "0.0.0-SNAPSHOT"
  :description "Logic pipeline manager."
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [org.clojure/clojure-contrib "1.2.0"]]
  :main org.timmc.pipeline.core
  :warn-on-reflection true
  :jar-name "pipeline.jar"
  :uberjar-name "pipeline-standalone.jar")

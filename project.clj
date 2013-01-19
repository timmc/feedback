(defproject org.timmc/feedback "0.4.0-SNAPSHOT"
  :description "Behavioral simulator utility for sequential logic circuits."
  :url "https://github.com/timmc/feedback"
  :author "Tim McCormack"
  :license [{:name "Eclipse Public License - v1.0"
             :url "http://www.eclipse.org/legal/epl-v10.html"
             :distribution :repo
             :comments "same as Clojure"}]
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [jkkramer/loom "0.2.0"]]
  :repl-options {:init-ns org.timmc.feedback})

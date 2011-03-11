(defproject org.timmc/feedback "0.1.0-SNAPSHOT"
  :description "Behavioral simulator utility for sequential logic circuits."
  :author "Tim McCormack"
  :license [{:name "Eclipse Public License - v1.0"
             :url "http://www.eclipse.org/legal/epl-v10.html"
             :distribution :repo
             :comments "same as Clojure"}
            {:name "GNU General Public License - v3.0"
             :url "http://www.gnu.org/licenses/gpl-3.0.html"
             :distribution :repo}]
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [org.satta/loom "0.1.0-SNAPSHOT"]]
  :dev-dependencies [[lein-clojars "0.6.0"]]
  ;;:warn-on-reflection true
  :main org.timmc.feedback)

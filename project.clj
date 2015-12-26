(defproject larva "0.1.0-SNAPSHOT"
  :description "Pretends to be a Clojure application generator."
  :url "https://github.com/novakboskov/larva"
  :author "Novak Boškov"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [prismatic/schema "1.0.4"]
                 [ubergraph "0.1.9"]
                 [rhizome "0.2.6-SNAPSHOT"]]
  :main ^:skip-aot larva.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})

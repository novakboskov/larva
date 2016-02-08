(defproject larva "0.1.0-SNAPSHOT"
  :description "Tends to be a Clojure application generator."
  :url "https://github.com/novakboskov/larva"
  :author "Novak Boškov"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [prismatic/schema "1.0.4"]
                 [ubergraph "0.1.9"]
                 [rhizome "0.2.6-SNAPSHOT"]
                 [yesql "0.5.1"]
                 [conman "0.2.9"]
                 [selmer "1.0.0"]
                 [leiningen "2.6.0"]]
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}
             :1.6     {:dependencies [[org.clojure/clojure "1.6.0"]]}
             :1.7     {:dependencies [[org.clojure/clojure "1.7.0"]]}
             :1.8     {:dependencies [[org.clojure/clojure "1.8.0"]]}}
  :aliases {"test-all" ["with-profile" "+1.6:+1.7:+1.8" "do" ["test"]]})

(ns larva.core-test
  (:require [clojure.test :refer :all]
            [larva.core :as sa]
            [ubergraph.core :as g]))

;;;;;;
;; Test data
;;;;;;

(def no-entities-edge-case
  {:about
   {:name "Pilot model"
    :author "Novak Boskov"
    :comment "This is just in the sake of a proof of concept."}
   :entities
   []})

(def no-entities-no-about-edge-case
  {:entities
   []})

(def standard-program-1
  {:about
   {:name "Pilot model"
    :author "Novak Boskov"
    :comment "This is just in the sake of a proof of concept."}
   :entities
   [{:signature "Musician"
     :properties [{:name "name" :type :str :gui-label "Name"}
                  {:name "surname" :type :str :gui-label "Surname"}
                  {:name "nickname" :type :str :gui-label "nick"}
                  {:name "honors" :type {:coll :str}}]}
    {:signature "Band"
     :properties [{:name "name" :type :str :gui-label "Name"}
                  {:name "genre" :type :str :gui-label "Genre"}
                  {:name "largeness" :type :str :gui-label "Largeness"}
                  {:name "members" :type {:coll :ref-to :signature "Musician"} :gui-label "Members"}]}
    {:signature "Festival"
     :properties [{:name "name" :type :str :gui-label "Name"}
                  {:name "location" :type :geo :gui-label "Loco"}]}]})

(def standard-program-11
  {:about
   {:name "Pilot model"
    :author "Novak Boskov"
    :comment "This is just in the sake of a proof of concept."}
   :entities
   [{:signature "Musician"
     :properties [{:name "name" :type :str :gui-label "Name"}
                  {:name "surename" :type :str :gui-label "Surname"}
                  {:name "nickname" :type :str :gui-label "nick"}
                  {:name "honors" :type {:coll :str}}]}
    {:signature "Band"
     :properties [{:name "name" :type :str :gui-label "Name"}
                  {:name "genre" :type :str :gui-label "Genre"}
                  {:name "largeness" :type :str :gui-label "Largeness"}
                  {:name "members" :type {:coll :ref-to :signature "Musician"} :gui-label "Members"}]}
    {:signature "Fan"
     :properties [{:name "name" :type :str :gui-label "Name"}
                  {:name "surname" :type :str :gui-label "Surname"}
                  {:name "gender" :type :str :gui-label "Gender"}
                  {:name "bands" :type {:coll :ref-to :signature "Band"} :gui-label "Beloved bands"}]}
    {:signature "Festival"
     :properties [{:name "name" :type :str :gui-label "Name"}
                  {:name "location" :type :geo :gui-label "Loco"}]}]})

(def standard-program-2
  {:entities
   [{:signature "Musician"
     :properties [{:name "name" :type :str :gui-label "Name"}
                  {:name "surename" :type :str :gui-label "Surname"}
                  {:name "nickname" :type :str :gui-label "nick"}
                  {:name "honors" :type {:coll :str}}]}
    {:signature "Band"
     :properties [{:name "name" :type :str :gui-label "Name"}
                  {:name "genre" :type :str :gui-label "Genre"}
                  {:name "largeness" :type :str :gui-label "Largeness"}
                  {:name "members" :type {:coll :ref-to :signature "Musician"} :gui-label "Members"}]}
    {:signature "Festival"
     :properties [{:name "name" :type :str :gui-label "Name"}
                  {:name "location" :type :geo :gui-label "Loco"}]}]})

;;;;;;
;; Tests
;;;;;;

(deftest root-successors-test
  (testing "Number of root node successors."
    (is (= 2 (-> (sa/to-graph standard-program-1)
                 (g/successors sa/root-node)
                 count)))
    (is (= 1 (-> (sa/to-graph standard-program-2)
                 (g/successors sa/root-node)
                 count)))))

(deftest entities-successors-test
  (testing "Number of entities-dispatch-node successors."
    (is (= 3 (-> (sa/to-graph standard-program-1)
                 (g/successors sa/entities-node)
                 count)))
    (is (= 4 (-> (sa/to-graph standard-program-11)
                 (g/successors sa/entities-node)
                 count)))
    (is (= 3 (-> (sa/to-graph standard-program-2)
                 (g/successors sa/entities-node)
                 count)))
    (is (= 0 (-> (sa/to-graph no-entities-edge-case)
                 (g/successors sa/entities-node)
                 count)))
    (is (= 0 (-> (sa/to-graph no-entities-no-about-edge-case)
                 (g/successors sa/entities-node)
                 count)))))

(deftest entity-property-test
  (testing "Name of properties of a specific entity."
    (is (= ["name" "surname" "nickname" "honors"]
           (let [g (sa/to-graph standard-program-1)
                 n "Musician"]
             (->> (g/successors g n)
                  (mapv #(:name (g/attrs g %)))))))
    (is (= ["name" "location"]
           (let [g (sa/to-graph standard-program-1)
                 n "Festival"]
             (->> (g/successors g n)
                  (mapv #(:name (g/attrs g %)))))))))

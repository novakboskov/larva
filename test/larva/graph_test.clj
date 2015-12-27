(ns larva.graph-test
  (:require [clojure.test :refer :all]
            [larva.graph :as lg]
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

(def standard-program-cardinality-1
  "Test case contains one to many, one to one cardinality and whole-part
  relationship."
  {:entities
   [{:signature "Musician"
     :properties [{:name "name" :type :str :gui-label "Name"}
                  {:name "surname" :type :str :gui-label "Surname"}
                  {:name "nickname" :type :str :gui-label "nick"}
                  {:name "honors" :type {:coll :str}}]}
    {:signature "Band"
     :properties [{:name "name" :type :str :gui-label "Name"}
                  {:name "genre" :type :str :gui-label "Genre"}
                  {:name "largeness" :type :str :gui-label "Largeness"}
                  {:name "members" :type {:coll :ref-to :signature "Musician"} :gui-label "Members"}
                  {:name "category" :type {:one :ref-to :signature "Category"} :gui-label "Category"}]}
    {:signature "Category"
     :properties [{:name "name" :type :str :gui-label "Name"}
                  {:name "subcategories" :type {:coll :ref-to :signature "Category"}
                   :gui-label "subcategories"}]}
    {:signature "Festival"
     :properties [{:name "name" :type :str :gui-label "Name"}
                  {:name "location" :type :geo :gui-label "Loco"}]}]})

(def references-1
  {:about
   {:name "Pilot model"
    :author "Novak Boskov"
    :comment "This is just in the sake of a proof of concept."}
   :entities
   [{:signature "Musician"
     :properties [{:name "name" :type :str :gui-label "Name"}
                  {:name "surname" :type :str :gui-label "Surname"}
                  {:name "nickname" :type :str :gui-label "nick"}
                  {:name "honors" :type {:coll :str}}
                  {:name "band" :type {:one :ref-to :signature "Band"}
                   :gui-label "Of band"}]}
    {:signature "Band"
     :properties [{:name "name" :type :str :gui-label "Name"}
                  {:name "genre" :type :str :gui-label "Genre"}
                  {:name "largeness" :type :str :gui-label "Largeness"}
                  {:name "members" :type {:coll :ref-to :signature "Musician"}
                   :gui-label "Members"}
                  {:name "category" :type {:one :ref-to :signature "Category"}
                   :gui-label "Category"}]}
    {:signature "Category"
     :properties [{:name "name" :type :str :gui-label "Name"}
                  {:name "subcategories" :type {:coll :ref-to :signature "Category"}
                   :gui-label "subcategories"}]}
    {:signature "Festival"
     :properties [{:name "name" :type :str :gui-label "Name"}
                  {:name "location" :type :geo :gui-label "Loco"}
                  {:name "bands" :type {:coll :ref-to :signature "Band"}
                   :gui-label "participant bands"}]}]})
;;;;;;
;; Tests
;;;;;;

(deftest root-successors-test
  (testing "Number of root node successors."
    (is (= 2 (-> (lg/to-graph standard-program-1)
                 (g/successors lg/root-node)
                 count)))
    (is (= 1 (-> (lg/to-graph standard-program-2)
                 (g/successors lg/root-node)
                 count)))))

(deftest entities-successors-test
  (testing "Number of entities-dispatch-node successors."
    (is (= 3 (-> (lg/to-graph standard-program-1)
                 (g/successors lg/entities-node)
                 count)))
    (is (= 4 (-> (lg/to-graph standard-program-11)
                 (g/successors lg/entities-node)
                 count)))
    (is (= 3 (-> (lg/to-graph standard-program-2)
                 (g/successors lg/entities-node)
                 count)))
    (is (= 0 (-> (lg/to-graph no-entities-edge-case)
                 (g/successors lg/entities-node)
                 count)))
    (is (= 0 (-> (lg/to-graph no-entities-no-about-edge-case)
                 (g/successors lg/entities-node)
                 count)))))

(deftest entity-property-test
  (testing "Name of properties of a specific entity."
    (is (= ["name" "surname" "nickname" "honors"]
           (let [g (lg/to-graph standard-program-1)
                 n "Musician"]
             (->> (g/successors g n)
                  (mapv #(:name (g/attrs g %)))))))
    (is (= ["name" "location"]
           (let [g (lg/to-graph standard-program-1)
                 n "Festival"]
             (->> (g/successors g n)
                  (mapv #(:name (g/attrs g %)))))))))

(deftest property-reference-test
  (testing "References of properties. References represent property-entity
relationships originating from cardinality of entities."
    (let [p1 {:name "subcategories" :type {:coll :ref-to :signature "Category"}
              :gui-label "subcategories"}
          p2 {:name "band" :type {:one :ref-to :signature "Band"}
              :gui-label "Of band"}
          p3 {:name "bands" :type {:coll :ref-to :signature "Band"}
              :gui-label "participant bands"}]
      (is (= "Category"
             (-> (lg/to-graph standard-program-cardinality-1)
                 (g/out-edges (lg/build-property-label p1 "Category"))
                 first :dest)))
      (is (= "Band"
             (-> (lg/to-graph references-1)
                 (g/out-edges (lg/build-property-label p2 "Musician"))
                 first :dest)))
      (is (= "Band"
             (-> (lg/to-graph references-1)
                 (g/out-edges (lg/build-property-label p3 "Festival"))
                 first :dest))))))

(deftest property-reference-cardinality-test
  (testing "Cardinality of the property-entity relationships."
    (let [p1 {:name "band" :type {:one :ref-to :signature "Band"}
              :gui-label "Of band"}
          p2 {:name "bands" :type {:coll :ref-to :signature "Band"}
              :gui-label "participant bands"}
          p3 {:name "members" :type {:coll :ref-to :signature "Musician"}
              :gui-label "Members"}
          g (lg/to-graph references-1)]
      (is (= 1 (->> {:src (lg/build-property-label p1 "Musician") :dest "Band"}
                    (g/find-edges g) count)))
      (is (= :one (->> {:src (lg/build-property-label p1 "Musician") :dest "Band"}
                       (g/find-edges g) first
                       ((fn [edge] [(:src edge) (:dest edge)]))
                       (g/attrs g) :cardinality)))
      (is (= 1 (->> {:src (lg/build-property-label p2 "Festival") :dest "Band"}
                    (g/find-edges g) count)))
      (is (= :coll
             (->> {:src (lg/build-property-label p2 "Festival") :dest "Band"}
                  (g/find-edges g) first
                  ((fn [edge] [(:src edge) (:dest edge)]))
                  (g/attrs g) :cardinality)))
      (is (= 1 (->> {:src (lg/build-property-label p3 "Band") :dest "Musician"}
                    (g/find-edges g) count)))
      (is (= :coll
             (->> {:src (lg/build-property-label p3 "Band") :dest "Musician"}
                  (g/find-edges g) first
                  ((fn [edge] [(:src edge) (:dest edge)]))
                  (g/attrs g) :cardinality))))))

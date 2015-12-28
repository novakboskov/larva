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

(def standard-program-no-refs
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
                  {:name "largeness" :type :str :gui-label "Largeness"}]}
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
  "Test case with one->many, many->one, many->many, one->one relationships."
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
                   :gui-label "Of band"}
                  {:name "social-profile" :type {:one :ref-to :signature
                                                 "SocialMediaProfile"}
                   :gui-label "profile"}]}
    {:signature "Band"
     :properties [{:name "name" :type :str :gui-label "Name"}
                  {:name "genre" :type :str :gui-label "Genre"}
                  {:name "largeness" :type :str :gui-label "Largeness"}
                  {:name "members" :type {:coll :ref-to :signature "Musician"}
                   :gui-label "Members"}
                  {:name "category" :type {:one :ref-to :signature "Category"}
                   :gui-label "Category"}
                  {:name "participated" :type {:coll :ref-to :signature "Festival"}
                   :gui-label "Participated in"}]}
    {:signature "Category"
     :properties [{:name "name" :type :str :gui-label "Name"}
                  {:name "subcategories" :type {:coll :ref-to :signature "Category"}
                   :gui-label "subcategories"}
                  {:name "bands" :type {:coll :ref-to :signature "Band"}
                   :gui-label "Bands of category"}]}
    {:signature "Festival"
     :properties [{:name "name" :type :str :gui-label "Name"}
                  {:name "location" :type :geo :gui-label "Loco"}
                  {:name "bands" :type {:coll :ref-to :signature "Band"}
                   :gui-label "participant bands"}]}
    {:signature "SocialMediaProfile"
     :properties [{:name "owner" :type {:one :ref-to :signature "Musician"}
                   :gui-label "Name"}
                  {:name "name" :type :str :gui-label "name"}
                  {:name "provider" :type :str :gui-label "provider"}]}]})

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
relationships with their cardinality."
    (let [p0 {:name "honors" :type {:coll :str}}
          p01 {:name "nickname" :type :str :gui-label "nick"}
          p1 {:name "subcategories" :type {:coll :ref-to :signature "Category"}
              :gui-label "subcategories"}
          p2 {:name "band" :type {:one :ref-to :signature "Band"}
              :gui-label "Of band"}
          p3 {:name "bands" :type {:coll :ref-to :signature "Band"}
              :gui-label "participant bands"}]
      (is (= 0
             (-> (lg/to-graph standard-program-no-refs)
                 (g/out-edges (lg/build-property-label p0 "Musician"))
                 count)))
      (is (= 0
             (-> (lg/to-graph standard-program-no-refs)
                 (g/out-edges (lg/build-property-label p01 "Musician"))
                 count)))
      ;; whole-part relationship
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
    (let [g (lg/to-graph references-1)
          p1 {:name "band" :type {:one :ref-to :signature "Band"}
              :gui-label "Of band"}
          p2 {:name "bands" :type {:coll :ref-to :signature "Band"}
              :gui-label "participant bands"}
          p3 {:name "members" :type {:coll :ref-to :signature "Musician"}
              :gui-label "Members"}]
      (let [edges (g/find-edges g {:src (lg/build-property-label p1 "Musician")
                                   :dest "Band"})]
        (is (= 1 (count edges)))
        (is (= :one (->> (first edges)
                         ((fn [edge] [(:src edge) (:dest edge)]))
                         (g/attrs g) :cardinality))))
      (let [edges (g/find-edges g {:src (lg/build-property-label p2 "Festival")
                                   :dest "Band"})]
        (is (= 1 (count edges)))
        (is (= :coll (->> (first edges)
                          ((fn [edge] [(:src edge) (:dest edge)]))
                          (g/attrs g) :cardinality))))
      (let [edges (g/find-edges g {:src (lg/build-property-label p3 "Band")
                                   :dest "Musician"})]
        (is (= 1 (count edges)))
        (is (= :coll (->> (first edges)
                          ((fn [edge] [(:src edge) (:dest edge)]))
                          (g/attrs g) :cardinality)))))))

(deftest cardinality-graph-presentation-test
  (testing "If cardinality expressed through uni-model is established in graph
 representation of the program."
    (let [g (lg/to-graph references-1)
          band-particip {:name "participated" :type {:coll :ref-to :signature
                                                     "Festival"}
                         :gui-label "Participated in"}
          fest-bands {:name "bands" :type {:coll :ref-to :signature "Band"}
                      :gui-label "participant bands"}
          mus-soc-prof {:name "social-profile" :type {:one :ref-to :signature
                                                      "SocialMediaProfile"}
                        :gui-label "profile"}
          prof-owner {:name "owner" :type {:one :ref-to :signature "Musician"}
                      :gui-label "Name"}
          band-categ {:name "category" :type {:one :ref-to :signature "Category"}
                      :gui-label "Category"}
          categ-bands {:name "bands" :type {:coll :ref-to :signature "Band"}
                       :gui-label "participant bands"}]
      ;; many->many
      (let [es-band->fest
            (g/find-edges g {:src (lg/build-property-label band-particip "Band")
                             :dest "Festival"})
            es-fest->band
            (g/find-edges g {:src (lg/build-property-label fest-bands "Festival")
                             :dest "Band"})]
        (is (= 1 (count es-band->fest)))
        (is (= :coll (:cardinality (g/attrs g (first es-band->fest)))))
        (is (= 1 (count es-fest->band)))
        (is (= :coll (:cardinality (g/attrs g (first es-fest->band))))))
      ;; one->one
      (let [es-mus->profile
            (g/find-edges g {:src (lg/build-property-label mus-soc-prof "Musician")
                             :dest "SocialMediaProfile"})
            es-profile->mus
            (g/find-edges g {:src (lg/build-property-label prof-owner
                                                           "SocialMediaProfile")
                             :dest "Musician"})]
        (is (= 1 (count es-mus->profile)))
        (is (= :one (:cardinality (g/attrs g (first es-mus->profile)))))
        (is (= 1 (count es-profile->mus)))
        (is (= :one (:cardinality (g/attrs g (first es-profile->mus))))))
      ;; one->many
      (let [es-band->categ
            (g/find-edges g {:src (lg/build-property-label band-categ "Band")
                             :dest "Category"})
            es-categ->band
            (g/find-edges g {:src (lg/build-property-label categ-bands "Category")
                             :dest "Band"})]
        (is (= 1 (count es-band->categ)))
        (is (= :one (:cardinality (g/attrs g (first es-band->categ)))))
        (is (= 1 (count es-categ->band)))
        (is (= :coll (:cardinality (g/attrs g (first es-categ->band)))))))))

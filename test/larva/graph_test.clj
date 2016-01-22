(ns larva.graph-test
  (:require [clojure.test :refer :all]
            [larva
             [graph :as lg]
             [test-data :refer :all]]
            [ubergraph.core :as g]))

(deftest root-successors-test
  (testing "Number of root node successors."
    (is (= 2 (-> (lg/to-graph standard-program-1)
                 (g/successors lg/root-node)
                 count)))
    (is (= 1 (-> (lg/to-graph standard-program-2)
                 (g/successors lg/root-node)
                 count)))
    (is (= 3 (-> (lg/to-graph standard-program-with-meta)
                 (g/successors lg/root-node)
                 count)))))

(deftest root-successors-names-test
  (testing "Root node successors names."
    (is (= #{"about" "meta" :entities} (-> (lg/to-graph standard-program-with-meta)
                                           (g/successors lg/root-node)
                                           set)))))

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
    (is (= #{"name" "surname" "nickname" "honors"}
           (let [g (lg/to-graph standard-program-1)
                 n "Musician"]
             (->> (g/successors g n)
                  (mapv #(:name (g/attrs g %)))
                  set))))
    (is (= #{"name" "location"}
           (let [g (lg/to-graph standard-program-1)
                 n "Festival"]
             (->> (g/successors g n)
                  (mapv #(:name (g/attrs g %)))
                  set))))))

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

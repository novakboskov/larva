(ns larva.graph-test
  (:require [clojure.test :refer :all]
            [larva
             [graph :as lg]
             [test-data :refer :all]]
            [ubergraph.core :as g]))

(deftest root-successors-test
  (testing "Number of root node successors."
    (is (= 2 (-> (lg/->graph standard-program-1)
                 (g/successors lg/root-node)
                 count)))
    (is (= 1 (-> (lg/->graph standard-program-2)
                 (g/successors lg/root-node)
                 count)))
    (is (= 3 (-> (lg/->graph standard-program-with-meta)
                 (g/successors lg/root-node)
                 count)))))

(deftest root-successors-names-test
  (testing "Root node successors names."
    (is (= #{:about :meta :entities} (-> (lg/->graph standard-program-with-meta)
                                         (g/successors lg/root-node)
                                         set)))))

(deftest entities-successors-test
  (testing "Number of entities-dispatch-node successors."
    (is (= 3 (-> (lg/->graph standard-program-1)
                 (g/successors lg/entities-node)
                 count)))
    (is (= 4 (-> (lg/->graph standard-program-11)
                 (g/successors lg/entities-node)
                 count)))
    (is (= 3 (-> (lg/->graph standard-program-2)
                 (g/successors lg/entities-node)
                 count)))
    (is (= 0 (-> (lg/->graph no-entities-edge-case)
                 (g/successors lg/entities-node)
                 count)))
    (is (= 0 (-> (lg/->graph no-entities-no-about-edge-case)
                 (g/successors lg/entities-node)
                 count)))))

(deftest entity-property-test
  (testing "Name of properties of a specific entity."
    (is (= #{"name" "surname" "nickname" "honors" "band"}
           (let [g (lg/->graph standard-program-1)
                 n "Musician"]
             (->> (g/successors g n)
                  (mapv #(g/attr g % :name))
                  set))))
    (is (= #{"name" "location"}
           (let [g (lg/->graph standard-program-1)
                 n "Festival"]
             (->> (g/successors g n)
                  (mapv #(g/attr g % :name))
                  set))))))

(deftest entity-plural-test
  (testing "Presence and names of plurals in entity definition."
    (let [g  (lg/->graph entities-with-signature-plural)
          g1 (lg/->graph standard-program-with-meta)]
      (is (= ["Bands" "Categories"]
             (mapv #(:plural (g/attrs g %)) ["Band" "Category"])))
      (is (not-any? #(contains? % :plural)
                    (map #(g/attrs g %) ["Festival" "SocialMediaProfile" "Musician"])))
      (is (not-any? #(contains? % :plural)
                    (map #(g/attrs g1 %) (g/successors g1 lg/entities-node)))))))

(deftest property-reference-test
  (testing "References of properties. References represent property-entity
relationships with their cardinality."
    (let [p0  {:name "honors" :type {:coll :str}}
          p01 {:name "nickname" :type :str :gui-label "nick"}
          p1  {:name      "subcategories" :type
               {:coll :reference
                :to   ["Category" "subcategories"]}
               :gui-label "subcategories"}
          p2  {:name "band" :type {:one :reference
                                   :to  ["Band" "members"]}}
          p3  {:name      "participants" :type {:coll :reference :to ["Band"]}
               :gui-label "participant bands"}]
      (is (= 0
             (-> (lg/->graph standard-program-no-refs)
                 (g/out-edges (lg/build-property-label p0 "Musician"))
                 count)))
      (is (= 0
             (-> (lg/->graph standard-program-no-refs)
                 (g/out-edges (lg/build-property-label p01 "Musician"))
                 count)))
      ;; whole-part relationship
      (is (= "Category"
             (-> (lg/->graph standard-program-cardinality-1)
                 (g/out-edges (lg/build-property-label p1 "Category"))
                 first :dest)))
      (is (= "Band"
             (-> (lg/->graph references-1)
                 (g/out-edges (lg/build-property-label p2 "Musician"))
                 first :dest)))
      (is (= "Band"
             (-> (lg/->graph references-1)
                 (g/out-edges (lg/build-property-label p3 "Festival"))
                 first :dest))))))

(deftest property-reference-cardinality-test
  (testing "Cardinality of the property-entity relationships."
    (let [g  (lg/->graph references-1)
          p1 {:name      "band" :type {:one :reference :to ["Band"]}
              :gui-label "Of band"}
          p2 {:name      "participants" :type {:coll :reference :to ["Band"]}
              :gui-label "participant bands"}
          p3 {:name      "members" :type {:coll :reference
                                          :to   ["Musician" "band"]}
              :gui-label "Members"}]
      (let [edges (g/find-edges g {:src  (lg/build-property-label p1 "Musician")
                                   :dest "Band"})]
        (is (= 1 (count edges)))
        (is (= :one (->> (first edges)
                         ((fn [edge] [(:src edge) (:dest edge)]))
                         (g/attrs g) :cardinality))))
      (let [edges (g/find-edges g {:src  (lg/build-property-label p2 "Festival")
                                   :dest "Band"})]
        (is (= 1 (count edges)))
        (is (= :coll (->> (first edges)
                          ((fn [edge] [(:src edge) (:dest edge)]))
                          (g/attrs g) :cardinality))))
      (let [edges (g/find-edges g {:src  (lg/build-property-label p3 "Band")
                                   :dest "Musician"})]
        (is (= 1 (count edges)))
        (is (= :coll (->> (first edges)
                          ((fn [edge] [(:src edge) (:dest edge)]))
                          (g/attrs g) :cardinality)))))))

(deftest cardinality-graph-presentation-test
  (testing "If cardinality expressed through uni-model is established in graph
 representation of the program."
    (let [g                 (lg/->graph references-1)
          band-particip     {:name      "participated" :type
                             {:coll :reference
                              :to   ["Festival" "participants"]}
                             :gui-label "Participated in"}
          fest-participants {:name      "participants" :type {:coll :reference
                                                              :to   ["Band"]}
                             :gui-label "participant bands"}
          mus-soc-prof      {:name      "social-profile" :type
                             {:one :reference
                              :to  ["SocialMediaProfile"]}
                             :gui-label "profile"}
          prof-owner        {:name      "owner" :type
                             {:one :reference :to ["Musician" "social-profile"]}
                             :gui-label "Name"}
          band-categ        {:name      "category" :type
                             {:one :reference :to ["Category" "bands"]}
                             :gui-label "Category"}
          categ-bands       {:name      "bands" :type
                             {:coll :reference :to ["Band"]}
                             :gui-label "Bands of category"}]
      ;; many->many
      (let [es-band->fest
            (g/find-edges g {:src  (lg/build-property-label band-particip "Band")
                             :dest "Festival"})
            es-fest->band
            (g/find-edges g {:src  (lg/build-property-label fest-participants
                                                            "Festival")
                             :dest "Band"})]
        (is (= 1 (count es-band->fest)))
        (is (= :coll (:cardinality (g/attrs g (first es-band->fest)))))
        (is (= 1 (count es-fest->band)))
        (is (= :coll (:cardinality (g/attrs g (first es-fest->band))))))
      ;; one->one
      (let [es-mus->profile
            (g/find-edges g {:src  (lg/build-property-label mus-soc-prof "Musician")
                             :dest "SocialMediaProfile"})
            es-profile->mus
            (g/find-edges g {:src  (lg/build-property-label prof-owner
                                                            "SocialMediaProfile")
                             :dest "Musician"})]
        (is (= 1 (count es-mus->profile)))
        (is (= :one (:cardinality (g/attrs g (first es-mus->profile)))))
        (is (= 1 (count es-profile->mus)))
        (is (= :one (:cardinality (g/attrs g (first es-profile->mus))))))
      ;; one->many
      (let [es-band->categ
            (g/find-edges g {:src  (lg/build-property-label band-categ "Band")
                             :dest "Category"})
            es-categ->band
            (g/find-edges g {:src  (lg/build-property-label categ-bands "Category")
                             :dest "Band"})]
        (is (= 1 (count es-band->categ)))
        (is (= :one (:cardinality (g/attrs g (first es-band->categ)))))
        (is (= 1 (count es-categ->band)))
        (is (= :coll (:cardinality (g/attrs g (first es-categ->band)))))))))

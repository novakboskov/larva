(ns larva.program-api-test
  (:require [clojure.test :refer :all]
            [larva
             [program-api :as api]
             [test-data :refer :all]]))

(defmacro eval-in-program-model-context [program & forms]
  "Evaluates s-expressions in context of certain program model."
  `(do (larva.program-api/reset-program-model ~program)
       ~@forms
       (larva.program-api/destroy-program-model)))

(deftest model->program-test
  (testing "Options and resolve order of model->program function"
    (eval-in-program-model-context
     standard-program-1
     (is (= #{"Musician" "Band" "Festival"} (set (api/all-entities)))))
    (eval-in-program-model-context
     standard-program-1
     (is (= #{"Musician" "Band" "Festival"}
            (-> {:model references-1} api/all-entities set))))
    (is (= #{"Musician" "Band" "Category" "Festival" "SocialMediaProfile"}
           (-> {:model references-1} api/all-entities set)))))

(deftest all-entities-test
  (testing "Names and order of entities present in program"
    (is (= 0 (-> {:model no-entities-edge-case}
                 api/all-entities
                 count)))
    (is (= 0 (-> {:model no-entities-no-about-edge-case}
                 api/all-entities
                 count)))
    (is (= ["Musician" "Band" "Festival"] (-> {:model standard-program-1}
                                              api/all-entities)))
    (is (= ["Musician" "Band" "Category" "Festival" "SocialMediaProfile"]
           (-> {:model references-1} api/all-entities)))))

(deftest entity-info-test
  (testing "Structure and content of info map originated from entity."
    (eval-in-program-model-context
     entities-with-signature-plural
     (is (= {:signature "Band" :plural "Bands"} (api/entity-info "Band")))
     (is (= {:signature "Musician"} (api/entity-info "Musician"))))
    (eval-in-program-model-context
     standard-program-2
     (is (= {:signature "Musician"} (api/entity-info "Musician")))
     (is (= {:signature "Band"} (api/entity-info "Band")))
     (is (= {:signature "Festival"} (api/entity-info "Festival"))))))

(deftest entity-properties-test
  (testing "Content and order of properties returned."
    (eval-in-program-model-context
     standard-program-2
     (is (= [{:name "name" :type :str :gui-label "Name"}
             {:name "surename" :type :str :gui-label "Surname"}
             {:name "nickname" :type :str :gui-label "nick"}
             {:name "honors" :type {:coll :str}}
             {:name "band" :type {:one :reference
                                  :to  ["Band" "members"]}}]
            (api/entity-properties "Musician")))
     (eval-in-program-model-context
      standard-program-cardinality-1
      (is (= [{:name "name" :type :str :gui-label "Name"}
              {:name "genre" :type :str :gui-label "Genre"}
              {:name "largeness" :type :str :gui-label "Largeness"}
              {:name      "members" :type {:coll :reference
                                           :to   ["Musician"]}
               :gui-label "Members"}
              {:name      "category" :type {:one :reference
                                            :to  ["Category" "bands"]}
               :gui-label "Category"}]
             (api/entity-properties "Band"))))
     (eval-in-program-model-context
      custom-property-datatype
      (is (= [{:name "name" :type :str :gui-label "Name"}
              {:name "location" :type "POINT" :gui-label "Loco"}
              {:name      "participants" :type {:coll :reference
                                                :to   ["Band" "participated"]
                                                :gui  :table-view}
               :gui-label "participant bands"}]
             (api/entity-properties "Festival")))))))

(deftest property-data-type-test
  (testing "Returned data type declared in property."
    (is (= :str (api/property-data-type
                 "Musician"
                 {:name "name" :type :str :gui-label "Name"}
                 {:model custom-property-datatype})))
    (is (= {:one :reference
            :to  ["Band" "members"]
            :gui :select-form}
           (api/property-data-type
            "Musician"
            {:name      "band" :type {:one :reference
                                      :to  ["Band" "members"]
                                      :gui :select-form}
             :gui-label "Of band"}
            {:model custom-property-datatype})))
    (eval-in-program-model-context
     custom-property-datatype
     (is (= :str (api/property-data-type
                  "Musician"
                  {:name "name" :type :str :gui-label "Name"})))
     (is (= {:one :reference
             :to  ["Band" "members"]
             :gui :select-form}
            (api/property-data-type
             "Musician"
             {:name      "band" :type {:one :reference
                                       :to  ["Band" "members"]
                                       :gui :select-form}
              :gui-label "Of band"}))))))

(deftest project-name-test
  (testing "Project name referred by project.clj"
    (is (= "larva" (api/project-name)))))

(deftest program-meta-data-test
  (testing "Returned meta data from program."
    (eval-in-program-model-context
     standard-program-with-meta
     (is (= {:api-only true :db {:type :postgres :sql :yesql}}
            (api/program-meta))))
    (is (= {:api-only true :db {:type :postgres :sql :yesql}}
           (api/program-meta {:model standard-program-with-meta})))
    (is (= {} (api/program-meta {:model no-entities-no-about-edge-case})))
    (eval-in-program-model-context
     no-entities-edge-case
     (is (= {} (api/program-meta))))
    (eval-in-program-model-context
     no-entities-no-about-empty-meta
     (is (= {} (api/program-meta))))))

(deftest program-about-test
  (testing "Returned about section of a program"
    (eval-in-program-model-context
     no-entities-no-about-empty-meta
     (is (= {} (api/program-about))))
    (is (= {} (api/program-about {:model no-entities-no-about-edge-case})))
    (eval-in-program-model-context
     standard-program-1
     (is (= {:name    "Pilot model" :author "Novak Boskov"
             :comment "This is just in the sake of a proof of concept."}
            (api/program-about))))))

(deftest property-reference-test
  (testing "Returned entities which are referenced from certain property."
    (eval-in-program-model-context
     custom-property-datatype
     (let [property {:name "instruments" :type {:coll :reference
                                                :to   ["Instrument" "players"]
                                                :gui  :table-view}}]
       (is (= {:many-to-many "Instrument" :back-property "players"}
              (api/property-reference "Musician" property))))
     (let [property {:name "players" :type {:coll :reference
                                            :to   ["Musician"]
                                            :gui  :table-view}}]
       (is (= {:many-to-many "Musician" :back-property "instruments"}
              (api/property-reference "Instrument" property))))
     (let [property {:name      "subcategories" :type
                     {:coll :reference
                      :to   ["Category" "subcategories"]
                      :gui  :table-view}
                     :gui-label "subcategories"}]
       (is (= {:many-to-many "Category" :back-property "subcategories" :recursive true}
              (api/property-reference "Category" property))))
     (let [property {:name "influenced" :type {:coll :reference
                                               :to   ["Band" "influenced"]
                                               :gui  :table-view}}]
       (is (= {:many-to-many "Band" :back-property "influenced" :recursive true}
              (api/property-reference "Band" property))))
     (let [property {:name "mentor" :type {:one :reference
                                           :to  ["Mentor" "learner"]
                                           :gui :select-form}}]
       (is (= {:one-to-one "Mentor" :back-property "learner"}
              (api/property-reference "Musician" property))))
     (let [property {:name "guru" :type {:one :reference
                                         :to  ["Musician" "guru"]
                                         :gui :select-form}}]
       (is (= {:one-to-one "Musician" :back-property "guru" :recursive true}
              (api/property-reference "Musician" property))))
     (let [property {:name      "band" :type {:one :reference
                                              :to  ["Band" "members"]
                                              :gui :select-form}
                     :gui-label "Of band"}]
       (is (= {:one-to-many "Band" :back-property "members"}
              (api/property-reference "Musician" property))))
     (let [property {:name      "members" :type {:coll :reference
                                                 :to   ["Musician"]}
                     :gui-label "Members"}]
       (is (= {:many-to-one "Musician" :back-property "band"}
              (api/property-reference "Band" property))))
     (let [property {:name "name" :type :str :gui-label "Name"}]
       (is (= :not-a-reference
              (api/property-reference "Musician" property)))))))

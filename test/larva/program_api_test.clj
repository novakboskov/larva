(ns larva.program-api-test
  (:require [clojure.test :refer :all]
            [larva
             [program-api :as api]
             [test-data :refer :all]]))

(defmacro eval-in-program-model-context [program & forms]
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
  (testing "Testing structure and content of info map originated from entity."
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
             {:name "honors" :type {:coll :str}}]
            (api/entity-properties "Musician")))
     (eval-in-program-model-context
      standard-program-cardinality-1
      (is (= [{:name "name" :type :str :gui-label "Name"}
              {:name "genre" :type :str :gui-label "Genre"}
              {:name "largeness" :type :str :gui-label "Largeness"}
              {:name "members" :type {:coll :ref-to :signature "Musician"} :gui-label "Members"}
              {:name "category" :type {:one :ref-to :signature "Category"} :gui-label "Category"}]
             (api/entity-properties "Band")))))))

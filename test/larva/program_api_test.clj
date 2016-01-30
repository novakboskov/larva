(ns larva.program-api-test
  (:require [clojure.test :refer :all]
            [larva
             [program-api :as api]
             [test-data :refer :all]]
            [larva.graph :as g]))

(defmacro eval-in-program-model-context [program & forms]
  `(do (larva.program-api/reset-program-model ~program)
       ~@forms
       (larva.program-api/destroy-program-model)))

(deftest all-entities-test
  (testing "Names of entities present in program"
    (is (= 0 (-> {:model no-entities-edge-case}
                 api/all-entities
                 count)))
    (is (= 0 (-> {:model no-entities-no-about-edge-case}
                 api/all-entities
                 count)))
    (is (= #{"Musician" "Band" "Festival"} (-> {:model standard-program-1}
                                               api/all-entities
                                               set)))
    (is (= #{"Musician" "Band" "Category" "Festival" "SocialMediaProfile"}
           (-> {:model references-1} api/all-entities set)))
    ;; TODO: This should support option which produce ordered result
    ))

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

(deftest entity-info-test
  ;; TODO:
  )

(deftest entity-properties-test
  ;; TODO: This should support option which produce ordered result
  )

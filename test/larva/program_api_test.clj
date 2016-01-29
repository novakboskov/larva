(ns larva.program-api-test
  (:require [clojure.test :refer :all]
            [larva
             [program-api :as pa]
             [test-data :refer :all]]))

(deftest all-entities-test
  (testing "Names of entities present in program"
    (is (= 0 (-> {:model no-entities-edge-case}
                 pa/all-entities
                 count)))
    (is (= 0 (-> {:model no-entities-no-about-edge-case}
                 pa/all-entities
                 count)))
    (is (= ["Musician" "Band" "Festival"] (-> {:model standard-program-1}
                                              pa/all-entities)))
    (is (= ["Musician" "Band" "Category" "Festival" "SocialMediaProfile"]
           (-> {:model references-1}
               pa/all-entities)))))

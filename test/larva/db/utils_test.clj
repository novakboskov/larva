(ns larva.db.utils-test
  (:require [clojure.test :refer :all]
            [larva
             [program-api :as api]
             [program-api-test :refer [eval-in-program-model-context]]
             [test-data :refer :all]]
            [larva.db.utils :as utils]))

(deftest build-sequence-string-test
  (testing "Building string for database."
    (eval-in-program-model-context
     standard-program-1
     (is (= "(name, genre, largeness, members)"
            (utils/build-sequence-string (api/entity-properties "Band") :values)))
     (is (= "(:name, :genre, :largeness, :members)"
            (utils/build-sequence-string (api/entity-properties "Band") :insert)))
     (is (= "name = :name, genre = :genre, largeness = :largeness, members = :members"
            (utils/build-sequence-string (api/entity-properties "Band") :set))))
    (eval-in-program-model-context
     entity-have-no-properties
     (is (= "()"
            (utils/build-sequence-string (api/entity-properties "Band") :values)))
     (is (= "()"
            (utils/build-sequence-string (api/entity-properties "Band") :insert)))
     (is (= ""
            (utils/build-sequence-string (api/entity-properties "Band") :set))))))

(deftest build-plural-for-entity-test
  (testing "Returned plural according to provided program."
    (eval-in-program-model-context
     entities-with-signature-plural
     (is (= "categories" (utils/build-plural-for-entity "Category" {})))
     (is (= "musicians" (utils/build-plural-for-entity "Musician" {}))))))

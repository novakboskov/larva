(ns larva.db.utils-test
  (:require [clojure.test :refer :all]
            [larva
             [program-api :as api]
             [program-api-test :refer [eval-in-program-model-context]]
             [test-data :refer :all]]
            [larva.db.utils :as utils]))

(deftest infer-db-type-test
  (testing "DB type inference. It does not test scenario where db type is inferred from project.clj."
    (is (= :postgres (utils/infer-db-type {:model standard-program-with-meta})))
    (eval-in-program-model-context
     standard-program-with-meta
     (is (= :postgres (utils/infer-db-type))))
    (eval-in-program-model-context
     entities-with-signature-plural
     (is (= nil (utils/infer-db-type))))))

(deftest build-sequence-string-test
  (testing "Building string for database."
    (eval-in-program-model-context
     standard-program-1
     (let [db-type (utils/infer-db-type)]
       (is (= "(name, genre, largeness, members)"
              (utils/build-sequence-string (api/entity-properties "Band")
                                           db-type :values)))
       (is (= "(:name, :genre, :largeness, :members)"
              (utils/build-sequence-string (api/entity-properties "Band")
                                           db-type :insert)))
       (is (= "name = :name, genre = :genre, largeness = :largeness, members = :members"
              (utils/build-sequence-string (api/entity-properties "Band")
                                           db-type :set)))))
    (eval-in-program-model-context
     entity-have-no-properties
     (let [db-type (utils/infer-db-type)]
       (is (= "()"
              (utils/build-sequence-string (api/entity-properties "Band")
                                           db-type :values)))
       (is (= "()"
              (utils/build-sequence-string (api/entity-properties "Band")
                                           db-type :insert)))
       (is (= ""
              (utils/build-sequence-string (api/entity-properties "Band")
                                           db-type :set)))))))

(deftest build-plural-for-entity-test
  (testing "Returned plural according to provided program."
    (eval-in-program-model-context
     entities-with-signature-plural
     (is (= "categories" (utils/build-plural-for-entity "Category" {})))
     (is (= "musicians" (utils/build-plural-for-entity "Musician" {}))))))

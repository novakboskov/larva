(ns larva.db.utils-test
  (:require [clojure
             [string :as cs]
             [test :refer :all]]
            [larva
             [program-api :as api]
             [program-api-test :refer [eval-in-program-model-context]]
             [test-data :refer :all]
             [utils :refer [slurp-as-data]]]
            [larva.db
             [stuff :as stuff]
             [utils :as utils]]))

(defmacro eval-in-environment [conf-for-type & forms]
  "Ensures execution in isolated environment considering database types
   definition file in larva source directory."
  `(let [stuff#      larva.db.stuff/database-types-config
         default#    larva.db.utils/default-db-data-types-config
         to-restore# (if (.exists default#) (larva.utils/slurp-as-data default#))]
     (if ~conf-for-type
       (larva.utils/spit-data default# {~conf-for-type (~conf-for-type stuff#)})
       (clojure.java.io/delete-file default# true))
     ~@forms
     (clojure.java.io/delete-file default#)
     (if to-restore# (larva.utils/spit-data default# to-restore#))))

(defn platform-agnostic
  "Makes platform independent string from Unix specific one."
  [str]
  (cs/replace str #"\\n" (System/lineSeparator)))

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
     (is (= "categories" (utils/build-plural-for-entity "Category")))
     (is (= "musicians" (utils/build-plural-for-entity "Musician"))))
    (is (= "categories" (utils/build-plural-for-entity
                         "Category" custom-property-datatype)))
    (eval-in-program-model-context
     custom-property-datatype
     (is (= "categories" (utils/build-plural-for-entity "Category"))))))

(deftest make-db-data-types-config-test
  (testing "database types configuration file on the right place.
 Do not test scenario when function is called with no parameters."
    (eval-in-environment
     false
     (is (= {:postgres (:postgres stuff/database-types-config)}
            (utils/make-db-data-types-config
             :spec {:model standard-program-with-meta}))))
    (eval-in-environment
     false
     (is (= {:mysql (:mysql stuff/database-types-config)}
            (utils/make-db-data-types-config :db-type :mysql))))
    (eval-in-environment
     :postgres
     (is (= {:h2 (:h2 stuff/database-types-config)}
            (utils/make-db-data-types-config :db-type :h2 :force true))))
    (eval-in-environment
     :postgres
     (is (= {:postgres (:postgres stuff/database-types-config)}
            (utils/make-db-data-types-config :db-type :h2))))))

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
       (larva.utils/spit-data default# (~conf-for-type stuff#))
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
     (is (= "categories" (utils/build-plural-for-entity "Category" {})))
     (is (= "musicians" (utils/build-plural-for-entity "Musician" {}))))))

(deftest make-db-data-types-config-test
  (testing "database types configuration file on the right place.
 Do not test scenario when function is called with no parameters."
    (eval-in-environment
     false
     (utils/make-db-data-types-config :spec {:model standard-program-with-meta})
     (is (= (:postgres stuff/database-types-config)
            (slurp-as-data utils/default-db-data-types-config))))
    (eval-in-environment
     false
     (utils/make-db-data-types-config :db-type :mysql)
     (is (= (:mysql stuff/database-types-config)
            (slurp-as-data utils/default-db-data-types-config))))
    (eval-in-environment
     :postgres
     (utils/make-db-data-types-config :db-type :h2 :force true)
     (is (= (:h2 stuff/database-types-config)
            (slurp-as-data utils/default-db-data-types-config))))
    (eval-in-environment
     :postgres
     (utils/make-db-data-types-config :db-type :h2)
     (is (= (:postgres stuff/database-types-config)
            (slurp-as-data utils/default-db-data-types-config))))))

(deftest build-db-create-table-string-test
  (testing "Create table string and properties with references returning."
    (eval-in-program-model-context
     custom-property-datatype
     (eval-in-environment
      :postgres
      (let [entity         (nth (api/all-entities) 0)
            entity-db-name (utils/drill-out-name-for-db entity)
            ps             (api/entity-properties entity)
            string         (platform-agnostic "(id serial PRIMARY KEY,\n name VARCHAR(30),\n surname VARCHAR(30),\n nickname VARCHAR(20),\n band INTEGER,\n social_profile INTEGER,\n guru INTEGER,\n mentor INTEGER)")]
        (is (= [string
                {entity-db-name
                 [{:name "honors" :type {:coll :str}}
                  {:name      "band"
                   :type      {:one :ref-to :signature "Band" :gui :select-form}
                   :gui-label "Of band"}
                  {:name      "social-profile"
                   :type      {:one :ref-to :signature "SocialMediaProfile"}
                   :gui-label "profile"}
                  {:name "instruments" :type {:coll      :ref-to
                                              :signature "Instrument"
                                              :gui       :table-view}}
                  {:name "guru" ,
                   :type {:one :ref-to, :signature "Musician" , :gui :select-form}}
                  {:name "mentor" ,
                   :type {:one :ref-to, :signature "Mentor" , :gui :select-form}}]}]
               (utils/build-db-create-table-string entity-db-name ps :postgres true))))))
    (eval-in-program-model-context
     custom-property-datatype
     (eval-in-environment
      :postgres
      (let [entity         (nth (api/all-entities) 1)
            entity-db-name (utils/drill-out-name-for-db entity)
            ps             (api/entity-properties entity)
            string         (platform-agnostic
                            "(id AUTO_INCREMENT PRIMARY KEY,\n name VARCHAR(30),\n genre VARCHAR(30),\n largeness INTEGER,\n category INTEGER)")]
        (is (= [string
                {entity-db-name
                 [{:name      "members" :type {:coll :ref-to :signature "Musician"}
                   :gui-label "Members"}
                  {:name      "category" :type {:one       :ref-to
                                                :signature "Category"
                                                :gui       :drop-list}
                   :gui-label "Category"}
                  {:name      "participated" :type {:coll      :ref-to
                                                    :signature "Festival"
                                                    :gui       :table-view}
                   :gui-label "Participated in"}
                  {:name "influenced" ,
                   :type {:coll :ref-to, :signature "Band" , :gui :table-view}}]}]
               (utils/build-db-create-table-string entity-db-name ps :mysql true))))))
    (eval-in-program-model-context
     custom-property-datatype
     (eval-in-environment
      :postgres
      (let [entity         (last (drop-last 1 (api/all-entities)))
            entity-db-name (utils/drill-out-name-for-db entity)
            ps             (api/entity-properties entity)
            string         (platform-agnostic
                            "(id AUTO_INCREMENT PRIMARY KEY,\n more_info VARCHAR(30))")]
        (is (= [string {entity-db-name []}]
               (utils/build-db-create-table-string entity-db-name ps :mysql true))))))))

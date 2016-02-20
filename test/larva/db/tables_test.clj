(ns larva.db.tables-test
  (:require [clojure.test :refer :all]
            [larva
             [program-api :as api]
             [program-api-test :refer [eval-in-program-model-context]]
             [test-data :refer [custom-property-datatype]]]
            [larva.db
             [tables :as tbl]
             [utils-test :refer [eval-in-environment platform-agnostic]]]))

(deftest build-db-create-table-string-test
  (testing "Create table string and properties with references returning."
    (eval-in-program-model-context
     custom-property-datatype
     (eval-in-environment
      :postgres
      (let [entity (nth (api/all-entities) 0)
            ps     (api/entity-properties entity)
            string (platform-agnostic "(id serial PRIMARY KEY,\n name VARCHAR(30),\n surname VARCHAR(30),\n nickname VARCHAR(20),\n band INTEGER,\n social_profile INTEGER,\n guru INTEGER,\n mentor INTEGER)")]
        (is (= [string
                {entity
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
               (tbl/build-db-create-table-string entity ps :postgres true))))))
    (eval-in-program-model-context
     custom-property-datatype
     (eval-in-environment
      :postgres
      (let [entity (nth (api/all-entities) 1)
            ps     (api/entity-properties entity)
            string (platform-agnostic
                    "(id AUTO_INCREMENT PRIMARY KEY,\n name VARCHAR(30),\n genre VARCHAR(30),\n largeness INTEGER,\n category INTEGER)")]
        (is (= [string
                {entity
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
               (tbl/build-db-create-table-string entity ps :mysql true))))))
    (eval-in-program-model-context
     custom-property-datatype
     (eval-in-environment
      :postgres
      (let [entity (last (drop-last 1 (api/all-entities)))
            ps     (api/entity-properties entity)
            string (platform-agnostic
                    "(id AUTO_INCREMENT PRIMARY KEY,\n more_info VARCHAR(30))")]
        (is (= [string {entity []}]
               (tbl/build-db-create-table-string entity ps :mysql true))))))))

(deftest build-additional-templates-keys-test
  (testing "Returned keys intended to fulfill create table template."
    (eval-in-program-model-context
     custom-property-datatype
     (let [ents      (api/all-entities)
           db-t      :postgres
           ent-props (map #(second
                            (tbl/build-db-create-table-string %1 %2 db-t false))
                          ents (map #(api/entity-properties %) ents))]
       (is (= ent-props (tbl/build-additional-templates-keys ent-props db-t nil)))))))

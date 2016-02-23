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
            string (platform-agnostic "(id serial PRIMARY KEY,\n name VARCHAR(30),\n surname VARCHAR(30),\n nickname VARCHAR(20),\n band INTEGER,\n dream_band INTEGER,\n social_profile INTEGER,\n instruments INTEGER,\n knows_how_to_repair INTEGER,\n guru INTEGER,\n disrespected_by INTEGER,\n mentor INTEGER)")]
        (is (= [string
                {"Musician"
                 [{:name "honors" :type {:coll :str}}
                  {:name      "band"
                   :type
                   {:one :reference :to ["Band" "members"] :gui :select-form}
                   :gui-label "Of band"}
                  {:name      "dream band"
                   :type
                   {:one :reference :to ["Band" "dream about"] :gui :select-form}
                   :gui-label "Dream about"}
                  {:name      "social-profile"
                   :type      {:one :reference :to ["SocialMediaProfile" "owner"]}
                   :gui-label "profile"}
                  {:name "instruments"
                   :type
                   {:coll :reference
                    :to   ["Instrument" "players"]
                    :gui  :table-view}}
                  {:name "knows how to repair"
                   :type
                   {:coll :reference
                    :to   ["Instrument" "repairers"]
                    :gui  :table-view}}
                  {:name "guru"
                   :type
                   {:one :reference :to ["Musician" "guru"] :gui :select-form}}
                  {:name "disrespected by"
                   :type
                   {:one :reference
                    :to  ["Mentor" "disrespect"]
                    :gui :select-form}}
                  {:name "mentor"
                   :type
                   {:one :reference
                    :to  ["Mentor" "learner"]
                    :gui :select-form}}]}]
               (tbl/build-db-create-table-string entity ps :postgres true nil))))))
    (let [entity (nth (api/all-entities) 1)
          ps     (api/entity-properties entity)
          string (platform-agnostic
                  "(id AUTO_INCREMENT PRIMARY KEY,\n name VARCHAR(30),\n genre VARCHAR(30),\n largeness INTEGER,\n category INTEGER,\n participated INTEGER,\n influenced INTEGER)")]
      (is (= [string
              {entity
               [{:name      "members" :type {:coll :reference
                                             :to   ["Musician"]}
                 :gui-label "Members"}
                {:name      "dream about" :type {:coll :reference
                                                 :to   ["Musician"]}
                 :gui-label "Members"}
                {:name      "category" :type {:one :reference
                                              :to  ["Category" "bands"]
                                              :gui :drop-list}
                 :gui-label "Category"}
                {:name      "participated" :type {:coll :reference
                                                  :to   ["Festival"]
                                                  :gui  :table-view}
                 :gui-label "Participated in"}
                {:name "influenced" :type {:coll :reference
                                           :to   ["Band" "influenced"]
                                           :gui  :table-view}}]}]
             (tbl/build-db-create-table-string entity ps :mysql true nil))))
    (let [entity (last (drop-last 1 (api/all-entities)))
          ps     (api/entity-properties entity)
          string (platform-agnostic
                  "(id AUTO_INCREMENT PRIMARY KEY,\n more_info VARCHAR(30))")]
      (is (= [string {entity []}]
             (tbl/build-db-create-table-string entity ps :mysql true nil))))))

(deftest make-create-tbl-keys-test
  (testing "Correctness of template keys contributed by make-create-table-keys."
    (eval-in-program-model-context
     custom-property-datatype
     (eval-in-environment
      :postgres
      (let [p0     {:name      "band" :type {:one :reference
                                             :to  ["Band" "members"]
                                             :gui :select-form}
                    :gui-label "Of band"}
            p1     {:name "instruments" :type {:coll :reference
                                               :to   ["Instrument" "players"]
                                               :gui  :table-view}}
            entity "Musician"
            crd0   (api/property-reference entity p0)
            crd1   (api/property-reference entity p1)]
        (is (= {} (tbl/make-create-tbl-keys crd0 entity p0 nil :postgres {})))
        (is (= {} (tbl/make-create-tbl-keys crd1 entity p1 nil :postgres {}))))))))

(deftest build-additional-templates-keys-test
  (testing "Returned keys intended to fulfill create table template."
    (eval-in-program-model-context
     custom-property-datatype
     (let [ents      (api/all-entities)
           db-t      :postgres
           ent-props (map #(second
                            (tbl/build-db-create-table-string %1 %2 db-t false
                                                              nil))
                          ents (map #(api/entity-properties %) ents))]
       (is (= ent-props (tbl/build-additional-templates-keys ent-props db-t nil)))))))

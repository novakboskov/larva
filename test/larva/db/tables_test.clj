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
            string (platform-agnostic "(id SERIAL PRIMARY KEY,\n name VARCHAR(30),\n surname VARCHAR(30),\n nickname VARCHAR(20),\n band INTEGER,\n dream_band INTEGER,\n social_profile INTEGER,\n instruments INTEGER,\n knows_how_to_repair INTEGER,\n disrespected_by INTEGER,\n mentor INTEGER)")]
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
               (tbl/build-db-create-table-string entity ps :postgres true nil)))))
     (eval-in-environment
      :mysql
      (let [entity (nth (api/all-entities) 1)
            ps     (api/entity-properties entity)
            string (platform-agnostic
                    "(id INTEGER AUTO_INCREMENT PRIMARY KEY,\n name VARCHAR(30),\n genre VARCHAR(30),\n largeness INTEGER,\n category INTEGER,\n participated INTEGER)")]
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
                    "(id INTEGER AUTO_INCREMENT PRIMARY KEY,\n more_info VARCHAR(30))")]
        (is (= [string {entity []}]
               (tbl/build-db-create-table-string entity ps :mysql true nil))))))))

(deftest make-create-tbl-keys-test
  (testing "Correctness of template keys contributed by make-create-table-keys."
    (eval-in-program-model-context
     custom-property-datatype
     (let [p0      {:name      "band" :type {:one :reference
                                             :to  ["Band" "members"]
                                             :gui :select-form}
                    :gui-label "Of band"}
           p1      {:name "instruments" :type {:coll :reference
                                               :to   ["Instrument" "players"]
                                               :gui  :table-view}}
           p2      {:name      "dream about" :type {:coll :reference
                                                    :to   ["Musician"]}
                    :gui-label "Members"}
           p3      {:name      "dream band" :type {:one :reference
                                                   :to  ["Band" "dream about"]
                                                   :gui :select-form}
                    :gui-label "Dream about"}
           p4      {:name "honors" :type {:coll :str}}
           p5      {:name "guru" :type {:one :reference
                                        :to  ["Musician" "guru"]
                                        :gui :select-form}}
           p6      {:name "influenced" :type {:coll :reference
                                              :to   ["Band" "influenced"]
                                              :gui  :table-view}}
           p7      {:name      "owner" :type {:one :reference
                                              :to  ["Musician"]
                                              :gui :select-form}
                    :gui-label "Name"}
           entity0 "Musician"
           entity1 "Band"
           entity3 "SocialMediaProfile"
           crd0    (api/property-reference entity0 p0)
           crd1    (api/property-reference entity0 p1)
           crd2    (api/property-reference entity1 p2)
           crd3    (api/property-reference entity0 p3)
           crd4    (api/property-reference entity0 p4)
           crd5    (api/property-reference entity0 p5)
           crd6    (api/property-reference entity1 p6)
           crd7    (api/property-reference entity3 p7)]
       (eval-in-environment
        :postgres
        (is (= {} (tbl/make-create-tbl-keys crd0 entity0 p0 nil {})))
        (is
         (= {:create-tables
             [{:ad-entity-plural
               "Musicians__instruments__Instruments__players__mtm"
               :ad-props-create-table
               " id SERIAL PRIMARY KEY,\n musicians_id INTEGER REFERENCES Musicians(id),\n musicians_id INTEGER REFERENCES Instruments(id)"}]}
            (tbl/make-create-tbl-keys crd1 entity0 p1 nil {})))
        (is (= {:create-tables
                [{:ad-entity-plural
                  "Socialmediaprofiles__owner__Musicians__social_profile__oto"
                  :ad-props-create-table
                  " id SERIAL PRIMARY KEY,\n socialmediaprofiles_id INTEGER REFERENCES Socialmediaprofiles(id) UNIQUE,\n socialmediaprofiles_id INTEGER REFERENCES Musicians(id) UNIQUE"}]}
               (tbl/make-create-tbl-keys crd7 entity3 p7 nil {}))))
       (eval-in-environment
        :mysql
        (is (= {} (tbl/make-create-tbl-keys crd2 entity1 p2 nil {})))
        (is (= {} (tbl/make-create-tbl-keys crd3 entity0 p3 nil {})))
        (is (= {:create-tables
                [{:ad-entity-plural "Musicians__honors__smpl_coll"
                  :ad-props-create-table
                  " id INTEGER AUTO_INCREMENT PRIMARY KEY,\n musicians_id INTEGER REFERENCES Musicians(id),\n honors VARCHAR(30)"}]}
               (tbl/make-create-tbl-keys crd4 entity0 p4 nil {})))
        (is (= {:create-tables
                [{:ad-entity-plural "Musicians__guru__r_oto"
                  :ad-props-create-table
                  " id INTEGER AUTO_INCREMENT PRIMARY KEY,\n musicians_id INTEGER REFERENCES Musicians(id),\n musicians_id_r INTEGER REFERENCES Musicians(id),\n UNIQUE(musicians_id, musicians_id_r)"}]}
               (tbl/make-create-tbl-keys crd5 entity0 p5 nil {})))
        (is (= {:create-tables
                [{:ad-entity-plural "Musicians__influenced__r_mtm" ,
                  :ad-props-create-table
                  " id INTEGER AUTO_INCREMENT PRIMARY KEY,\n musicians_id INTEGER REFERENCES Musicians(id),\n musicians_id_r INTEGER REFERENCES Musicians(id)"}]}
               (tbl/make-create-tbl-keys crd6 entity0 p6 nil {}))))))))

(deftest make-drop-tbl-keys-test
  (testing "Correctness of template keys contributed by make-drop-tbl-keys."
    (eval-in-program-model-context
     custom-property-datatype
     (eval-in-environment
      :postgres
      (let [p0      {:name      "band" :type {:one :reference
                                              :to  ["Band" "members"]
                                              :gui :select-form}
                     :gui-label "Of band"}
            p1      {:name "honors" :type {:coll :str}}
            p2      {:name      "members" :type {:coll :reference
                                                 :to   ["Musician"]}
                     :gui-label "Members"}
            p3      {:name      "participated" :type {:coll :reference
                                                      :to   ["Festival"]
                                                      :gui  :table-view}
                     :gui-label "Participated in"}
            p4      {:name "disrespected by" :type {:one :reference
                                                    :to  ["Mentor" "disrespect"]
                                                    :gui :select-form}}
            p5      {:name      "subcategories" :type
                     {:coll :reference
                      :to   ["Category" "subcategories"]
                      :gui  :table-view}
                     :gui-label "subcategories"}
            p6      {:name "guru" :type {:one :reference
                                         :to  ["Musician" "guru"]
                                         :gui :select-form}}
            entity0 "Musician"
            entity1 "Band"
            entity2 "Category"
            crd0    (api/property-reference entity0 p0)
            crd1    (api/property-reference entity0 p1)
            crd2    (api/property-reference entity1 p2)
            crd3    (api/property-reference entity1 p3)
            crd4    (api/property-reference entity0 p4)
            crd5    (api/property-reference entity2 p5)
            crd6    (api/property-reference entity0 p6)]
        (is (= {} (tbl/make-drop-tbl-keys crd0 entity0 p0 nil {})))
        (is (= {} (tbl/make-drop-tbl-keys crd2 entity1 p2 nil {})))
        (is (= {:drops [{:ad-entity-plural "Musicians__honors__smpl_coll"}]}
               (tbl/make-drop-tbl-keys crd1 entity0 p1 nil {})))
        (is (= {:drops [{:ad-entity-plural "Bands__participated__Festivals__participants__mtm"}]}
               (tbl/make-drop-tbl-keys crd3 entity1 p3 nil {})))
        (is (= {:drops [{:ad-entity-plural "Musicians__disrespected_by__Mentors__disrespect__oto"}]}
               (tbl/make-drop-tbl-keys crd4 entity0 p4 nil {})))
        (is (= {:drops [{:ad-entity-plural "Categories__subcategories__r_mtm"}]}
               (tbl/make-drop-tbl-keys crd5 entity2 p5 nil {})))
        (is (= {:drops [{:ad-entity-plural "Musicians__guru__r_oto"}]}
               (tbl/make-drop-tbl-keys crd6 entity0 p6 nil {}))))))))

;; (deftest build-additional-templates-keys-test
;;   (testing "Returned keys intended to fulfill create table template."
;;     (eval-in-program-model-context
;;      custom-property-datatype
;;      (let [ents      (api/all-entities)
;;            db-t      :postgres
;;            ent-props (map #(second
;;                             (tbl/build-db-create-table-string %1 %2 db-t false
;;                                                               nil))
;;                           ents (map #(api/entity-properties %) ents))]
;;        (is (= ent-props (tbl/build-additional-templates-keys ent-props db-t nil)))))))

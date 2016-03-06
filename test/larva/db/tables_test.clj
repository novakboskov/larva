(ns larva.db.tables-test
  (:require [clojure.test :refer :all]
            [larva
             [program-api :as api]
             [program-api-test :refer [eval-in-program-model-context]]
             [test-data :refer :all]]
            [larva.code-gen.common :as common]
            [larva.db
             [tables :as tbl]
             [utils-test :refer [eval-in-environment platform-agnostic]]]
            [larva.frameworks.luminus.stuff :as stuff]))

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
                   :gui-label "Dream about"}
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

(deftest partial-key-generation-test
  (testing "Correctness of template keys contributed by make-create-table-keys
            make-queries-keys, and make-alter-tbl-keys."
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
                    :gui-label "Dream about"}
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
           p8      {:name      "members" :type {:coll :reference
                                                :to   ["Musician"]}
                    :gui-label "Members"}
           p9      {:name "disrespected by" :type {:one :reference
                                                   :to  ["Mentor" "disrespect"]
                                                   :gui :select-form}}
           entity0 "Musician"
           entity1 "Band"
           entity2 "SocialMediaProfile"
           crd0    (api/property-reference entity0 p0)
           crd1    (api/property-reference entity0 p1)
           crd2    (api/property-reference entity1 p2)
           crd3    (api/property-reference entity0 p3)
           crd4    (api/property-reference entity0 p4)
           crd5    (api/property-reference entity0 p5)
           crd6    (api/property-reference entity1 p6)
           crd7    (api/property-reference entity2 p7)
           crd8    (api/property-reference entity1 p8)
           crd9    (api/property-reference entity0 p9)
           templates
           "resources/templates/"
           templ-yesql
           (str templates (second ((:additional-queries
                                    (stuff/relational-db-files)) :yesql)))
           templ-hugsql
           (str templates (second ((:additional-queries
                                    (stuff/relational-db-files)) :hugsql)))]
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
               (tbl/make-create-tbl-keys crd7 entity2 p7 nil {})))
        ;; make-alter-tbl-keys test
        (is (= {:alter-tables [{:table    "Musicians"
                                :fk-name  "FK__Musicians__Bands__band"
                                :on       "band"
                                :to-table "Bands"}]}
               (tbl/make-alter-tbl-keys crd8 entity1 p8 nil {})
               (tbl/make-alter-tbl-keys crd0 entity0 p0 nil {})))
        (is (= {:alter-tables [{:table    "Musicians"
                                :fk-name  "FK__Musicians__Bands__dream_band"
                                :on       "dream_band"
                                :to-table "Bands"}]}
               (tbl/make-alter-tbl-keys crd3 entity0 p3 nil {})
               (tbl/make-alter-tbl-keys crd2 entity1 p2 nil {})))
        (is (= {} (tbl/make-alter-tbl-keys crd7 entity2 p7 nil {})))
        (is (= {} (tbl/make-alter-tbl-keys crd1 entity0 p1 nil {})))
        (is (= {} (tbl/make-alter-tbl-keys crd4 entity0 p4 nil {})))
        (is (= {} (tbl/make-alter-tbl-keys crd5 entity0 p5 nil {})))
        (is (= {} (tbl/make-alter-tbl-keys crd6 entity1 p6 nil {})))
        ;; make-queries-keys test
        (is (= "-- name: get-musician-band<!\n-- returns band associated with musician\nSELECT * FROM Bands WHERE id = (SELECT band FROM Musicians WHERE id = :musician)\n\n-- name: get-band-members<!\n-- returns members associated with band\nSELECT * FROM Musicians WHERE band = :band\n\n\n"
               (common/render-template
                (slurp templ-hugsql) (tbl/make-queries-keys crd0 entity0 p0 nil {}))))
        (is (= "-- name: get-musician-instruments<!\n-- returns instruments associated with musician\nSELECT * FROM Instruments WHERE id IN (SELECT instrument_id FROM Musicians__instruments__Instruments__players__mtm WHERE musician_id = :musician)\n\n-- name: get-instrument-players<!\n-- returns players associated with instrument\nSELECT * FROM Musicians WHERE id IN (SELECT musician_id FROM Musicians__instruments__Instruments__players__mtm WHERE instrument_id = :instrument)\n\n\n"
               (common/render-template
                (slurp templ-yesql) (tbl/make-queries-keys crd1 entity0 p1 nil {}))))
        (is (= "-- name: get-musician-disrespected-by<!\n-- returns disrespected-by associated with musician\nSELECT * FROM Mentors WHERE id IN (SELECT mentor_id FROM Musicians__disrespected_by__Mentors__disrespect__oto WHERE musician_id = :musician)\n\n-- name: get-mentor-disrespect<!\n-- returns disrespect associated with mentor\nSELECT * FROM Musicians WHERE id IN (SELECT musician_id FROM Musicians__disrespected_by__Mentors__disrespect__oto WHERE mentor_id = :mentor)\n\n\n"
               (common/render-template
                (slurp templ-yesql) (tbl/make-queries-keys crd9 entity0 p9 nil {}))))
        (is (= "-- name: get-musician-guru<!\n-- returns guru associated with musician\nSELECT * FROM Musicians WHERE id = (SELECT guru_id FROM Musicians__guru__r_oto WHERE guru_id_r = :musician)\n\n\n"
               (common/render-template
                (slurp templ-yesql) (tbl/make-queries-keys crd5 entity0 p5 nil {}))))
        (is (= "-- name: get-musician-influenced<!\n-- returns influenced associated with musician\nSELECT * FROM Musicians WHERE id IN (SELECT influenced_id FROM Musicians__influenced__r_mtm WHERE influenced_id_r = :musician)\n\n\n"
               (common/render-template
                (slurp templ-yesql) (tbl/make-queries-keys crd6 entity0 p6 nil {}))))
        (is (= "-- name: get-musician-honors<!\n-- returns honors associated with musician\nSELECT * FROM Musicians__honors__smpl_coll WHERE musician_id = :musician\n\n\n"
               (common/render-template
                (slurp templ-yesql) (tbl/make-queries-keys crd4 entity0 p4 nil {})))))
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
               (tbl/make-create-tbl-keys crd6 entity0 p6 nil {})))
        ;; make-alter-tbl-keys test
        (is (= {:alter-tables [{:table    "Musicians"
                                :fk-name  "FK__Musicians__Bands__band"
                                :on       "band"
                                :to-table "Bands"}]}
               (tbl/make-alter-tbl-keys crd8 entity1 p8 nil {})
               (tbl/make-alter-tbl-keys crd0 entity0 p0 nil {})))
        (is (= {:alter-tables [{:table    "Musicians"
                                :fk-name  "FK__Musicians__Bands__dream_band"
                                :on       "dream_band"
                                :to-table "Bands"}]}
               (tbl/make-alter-tbl-keys crd3 entity0 p3 nil {})
               (tbl/make-alter-tbl-keys crd2 entity1 p2 nil {})))
        (is (= {} (tbl/make-alter-tbl-keys crd7 entity2 p7 nil {})))
        (is (= {} (tbl/make-alter-tbl-keys crd1 entity0 p1 nil {})))
        (is (= {} (tbl/make-alter-tbl-keys crd4 entity0 p4 nil {})))
        (is (= {} (tbl/make-alter-tbl-keys crd5 entity0 p5 nil {})))
        (is (= {} (tbl/make-alter-tbl-keys crd6 entity0 p6 nil {}))))))))

;; (deftest build-additional-templates-keys-test
;;   (testing "Returned keys intended to fulfill create table template."
;;     (eval-in-program-model-context
;;      entity-have-no-properties
;;      (let [ents      (api/all-entities)
;;            db-t      :postgres
;;            ent-props (map #(second
;;                             (tbl/build-db-create-table-string %1 %2 db-t false
;;                                                               nil))
;;                           ents (map #(api/entity-properties %) ents))]
;;        (is (= {} (tbl/build-additional-templates-keys ent-props nil)))))))

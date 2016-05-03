(ns larva.db.tables-test
  (:require [clojure.test :refer :all]
            [larva
             [program-api :as api]
             [program-api-test :refer [eval-in-program-model-context]]
             [test-data :refer :all]
             [test-results :as res]]
            [larva.code-gen.common :as common]
            [larva.db
             [tables :as tbl]
             [utils-test :refer [eval-in-environment platform-agnostic]]]
            [larva.frameworks.luminus.stuff :as stuff]))

;; Tests related to SQL for main tables

(deftest build-db-create-table-string-test
  (testing "Create table string and returning properties with references."
    (eval-in-program-model-context
     custom-property-datatype
     (eval-in-environment
      :postgres
      (let [entity (nth (api/all-entities) 0)
            ps     (api/entity-properties entity)
            string (platform-agnostic "(id SERIAL PRIMARY KEY,\n name VARCHAR(30),\n surname VARCHAR(30),\n nickname VARCHAR(20),\n band INTEGER,\n dream_band INTEGER)")]
        (is (= [string
                {entity
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
                    :gui :select-form}}]}
                {:needed-columns
                 [{:name "name", :type :str, :gui-label "Name"}
                  {:name "surname", :type :str, :gui-label "Surname"}
                  {:name "nickname", :type "VARCHAR(20)", :gui-label "nick"}
                  {:name      "band",
                   :type
                   {:one :reference, :to ["Band" "members"], :gui :select-form},
                   :gui-label "Of band"}
                  {:name      "dream band",
                   :type
                   {:one :reference, :to ["Band" "dream about"], :gui :select-form},
                   :gui-label "Dream about"}],
                 :entity entity}]
               (tbl/build-db-create-table-string entity ps :postgres true nil)))))
     (eval-in-environment
      :mysql
      (let [entity (nth (api/all-entities) 1)
            ps     (api/entity-properties entity)
            string (platform-agnostic "(id INTEGER AUTO_INCREMENT PRIMARY KEY,\n name VARCHAR(30),\n genre VARCHAR(30),\n largeness INTEGER,\n category INTEGER)")]
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
                                             :gui  :table-view}}]}
                {:needed-columns
                 [{:name "name", :type :str, :gui-label "Name"}
                  {:name "genre", :type :str, :gui-label "Genre"}
                  {:name "largeness", :type :num, :gui-label "Largeness"}
                  {:name      "category",
                   :type
                   {:one :reference, :to ["Category" "bands"], :gui :drop-list},
                   :gui-label "Category"}],
                 :entity entity}]
               (tbl/build-db-create-table-string entity ps :mysql true nil))))
      (let [entity (last (drop-last 1 (api/all-entities)))
            ps     (api/entity-properties entity)
            string (platform-agnostic
                    "(id INTEGER AUTO_INCREMENT PRIMARY KEY,\n more_info VARCHAR(30))")]
        (is (= [string {entity []}
                {:needed-columns [{:name "more info", :type :str}],
                 :entity         "more info"}]
               (tbl/build-db-create-table-string entity ps :mysql true nil))))))))

;; Tests related to SQL for relation-consequential tables.

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
           p9      {:name "disrespect" :type {:one :reference
                                              :to  ["Musician"]
                                              :gui :select-form}}
           entity0 "Musician"
           entity1 "Band"
           entity2 "SocialMediaProfile"
           entity3 "Mentor"
           crd0    (api/property-reference entity0 p0)
           crd1    (api/property-reference entity0 p1)
           crd2    (api/property-reference entity1 p2)
           crd3    (api/property-reference entity0 p3)
           crd4    (api/property-reference entity0 p4)
           crd5    (api/property-reference entity0 p5)
           crd6    (api/property-reference entity1 p6)
           crd7    (api/property-reference entity2 p7)
           crd8    (api/property-reference entity1 p8)
           crd9    (api/property-reference entity3 p9)
           templates
           "resources/templates/"
           templ-yesql
           (str templates (second ((:additional-queries
                                    stuff/relational-db-files) :yesql)))
           templ-hugsql
           (str templates (second ((:additional-queries
                                    stuff/relational-db-files) :hugsql)))]
       (eval-in-environment
        :postgres
        (is (= {} (tbl/make-create-tbl-keys crd0 entity0 p0 nil {})))
        (is
         (= {:create-tables
             [{:ad-entity-plural
               "Musicians__instruments__Instruments__players__mtm"
               :ad-props-create-table
               "(id SERIAL PRIMARY KEY,\n musicians_id INTEGER REFERENCES Musicians(id),\n instruments_id INTEGER REFERENCES Instruments(id))"}]}
            (tbl/make-create-tbl-keys crd1 entity0 p1 nil {})))
        (is (= {:create-tables
                [{:ad-entity-plural
                  "Socialmediaprofiles__owner__Musicians__social_profile__oto"
                  :ad-props-create-table
                  "(id SERIAL PRIMARY KEY,\n socialmediaprofiles_id INTEGER REFERENCES Socialmediaprofiles(id) UNIQUE,\n musicians_id INTEGER REFERENCES Musicians(id) UNIQUE)"}]}
               (tbl/make-create-tbl-keys crd7 entity2 p7 nil {})))
        (is (= {:create-tables
                [{:ad-entity-plural "Bands__influenced__r_mtm" ,
                  :ad-props-create-table
                  "(id SERIAL PRIMARY KEY,\n bands_id INTEGER REFERENCES Bands(id),\n bands_id_r INTEGER REFERENCES Bands(id))"}]}
               (tbl/make-create-tbl-keys crd6 entity1 p6 nil {})))
        (is (= {:create-tables
                [{:ad-entity-plural "Musicians__guru__r_oto" ,
                  :ad-props-create-table
                  "(id SERIAL PRIMARY KEY,\n musicians_id INTEGER REFERENCES Musicians(id) UNIQUE,\n musicians_id_r INTEGER REFERENCES Musicians(id) UNIQUE)"}]}
               (tbl/make-create-tbl-keys crd5 entity0 p5 nil {})))
        ;; make-alter-tbl-keys test
        (is (= {:alter-tables [{:table           "Musicians"
                                :fk-name         "FK__Musicians__Bands__band"
                                :on              "band"
                                :to-table        "Bands"
                                :drop-constraint "CONSTRAINT"}]}
               (tbl/make-alter-tbl-keys crd8 entity1 p8 nil {})
               (tbl/make-alter-tbl-keys crd0 entity0 p0 nil {})))
        (is (= {:alter-tables [{:table           "Musicians"
                                :fk-name         "FK__Musicians__Bands__dream_band"
                                :on              "dream_band"
                                :to-table        "Bands"
                                :drop-constraint "CONSTRAINT"}]}
               (tbl/make-alter-tbl-keys crd3 entity0 p3 nil {})
               (tbl/make-alter-tbl-keys crd2 entity1 p2 nil {})))
        (is (= {} (tbl/make-alter-tbl-keys crd7 entity2 p7 nil {})))
        (is (= {} (tbl/make-alter-tbl-keys crd1 entity0 p1 nil {})))
        (is (= {} (tbl/make-alter-tbl-keys crd4 entity0 p4 nil {})))
        (is (= {} (tbl/make-alter-tbl-keys crd5 entity0 p5 nil {})))
        (is (= {} (tbl/make-alter-tbl-keys crd6 entity1 p6 nil {})))

        ;; make-queries-keys test
        ;; A Musician-Band one-to-many relationship, "one" side
        (let [queries
              "-- :name get-musician-band :? :1\n-- :doc returns band associated with musician\nSELECT * FROM Bands\nWHERE id = (SELECT band FROM Musicians WHERE id = :musician)\n\n-- :name get-band-members :? :*\n-- :doc returns members associated with band\nSELECT * FROM Musicians\nWHERE band = :band\n\n-- :name assoc-musician-band! :!\n-- :doc associates musician with corresponding band\nUPDATE Musicians SET band = :band\nWHERE id = :musician\n\n-- :name assoc-band-members! :!\n-- :doc associates band with corresponding members\nUPDATE Musicians SET band = :band\nWHERE id IN :tuple:members\n\n-- :name dissoc-musician-band! :!\n-- :doc dissociates musician from corresponding band\nUPDATE Musicians\nSET band = NULL\nWHERE id = :musician\n\n-- :name dissoc-band-members! :!\n-- :doc dissociates band from corresponding members\nUPDATE Musicians\nSET band = NULL\nWHERE id IN :tuple:members\n\n-- :name dissoc-all-band-members! :!\n-- :doc dissociates all band from corresponding members\nUPDATE Musicians\nSET band = NULL\nWHERE band = :band\n\n\n"]
          (is (= queries
                 (common/render-template
                  (slurp templ-hugsql) (tbl/make-queries-keys crd0 entity0 p0 nil {}))))
          ;; other side of the same relation
          (is (= queries
                 (common/render-template
                  (slurp templ-hugsql) (tbl/make-queries-keys crd8 entity1 p8 nil {})))))
        ;; A Musician-Instrument many-to-many relationship
        (is (= "-- :name get-musician-instruments :? :*\n-- :doc returns instruments associated with musician\nSELECT * FROM Instruments\nWHERE id IN (SELECT instruments_id FROM Musicians__instruments__Instruments__players__mtm WHERE musicians_id = :musician)\n\n-- :name get-instrument-players :? :*\n-- :doc returns players associated with instrument\nSELECT * FROM Musicians\nWHERE id IN (SELECT musicians_id FROM Musicians__instruments__Instruments__players__mtm WHERE instruments_id = :instrument)\n\n-- :name assoc-musician-instruments! :!\n-- :doc associates musician with corresponding instruments\nINSERT INTO Musicians__instruments__Instruments__players__mtm (musicians_id, instruments_id)\nVALUES \n/*~\n(let [single (:musician params)]\n    (clojure.string/join\n        \", \" (for [m (:instruments params)]\"(\" single \", \" m \")\")))\n~*/\n\n-- :name assoc-instrument-players! :!\n-- :doc associates instrument with corresponding players\nINSERT INTO Musicians__instruments__Instruments__players__mtm (instruments_id, musicians_id)\nVALUES \n/*~\n(let [single (:instrument params)]\n    (clojure.string/join\n        \", \" (for [m (:players params)]\"(\" single \", \" m \")\")))\n~*/\n\n-- :name dissoc-musician-instruments! :!\n-- :doc dissociates musician from corresponding instruments\nDELETE FROM Musicians__instruments__Instruments__players__mtm\nWHERE musicians_id = :musician AND instruments_id IN :tuple:instruments\n\n-- :name dissoc-instrument-players! :!\n-- :doc dissociates instrument from corresponding players\nDELETE FROM Musicians__instruments__Instruments__players__mtm\nWHERE instruments_id = :instrument AND musicians_id IN :tuple:players\n\n-- :name dissoc-all-instrument-players! :!\n-- :doc dissociates all instrument from corresponding players\nDELETE FROM Musicians__instruments__Instruments__players__mtm\nWHERE instruments_id = :instrument\n\n-- :name dissoc-all-musician-instruments! :!\n-- :doc dissociates all musician from corresponding instruments\nDELETE FROM Musicians__instruments__Instruments__players__mtm\nWHERE musicians_id = :musician\n\n\n"
               (common/render-template
                (slurp templ-hugsql) (tbl/make-queries-keys crd1 entity0 p1 nil {}))))
        ;; A Musician-Mentor one-to-one relationship, Mentor's side
        (is (= "-- :name get-mentor-disrespect :? :1\n-- :doc returns disrespect associated with mentor\nSELECT * FROM Musicians\nWHERE id = (SELECT musicians_id FROM Mentors__disrespect__Musicians__disrespected_by__oto WHERE mentors_id = :mentor)\n\n-- :name get-musician-disrespected-by :? :1\n-- :doc returns disrespected-by associated with musician\nSELECT * FROM Mentors\nWHERE id = (SELECT mentors_id FROM Mentors__disrespect__Musicians__disrespected_by__oto WHERE musicians_id = :musician)\n\n-- :name assoc-mentor-disrespect! :!\n-- :doc associates mentor with corresponding disrespect\nINSERT INTO Mentors__disrespect__Musicians__disrespected_by__oto (mentors_id, musicians_id)\nVALUES (:mentor, :disrespect)\n\n-- :name assoc-musician-disrespected-by! :!\n-- :doc associates musician with corresponding disrespected-by\nINSERT INTO Mentors__disrespect__Musicians__disrespected_by__oto (musicians_id, mentors_id)\nVALUES (:musician, :disrespected-by)\n\n-- :name dissoc-mentor-disrespect! :!\n-- :doc dissociates mentor from corresponding disrespect\nDELETE FROM Mentors__disrespect__Musicians__disrespected_by__oto\nWHERE mentors_id = :mentor AND musicians_id = :disrespect\n\n-- :name dissoc-musician-disrespected-by! :!\n-- :doc dissociates musician from corresponding disrespected-by\nDELETE FROM Mentors__disrespect__Musicians__disrespected_by__oto\nWHERE musicians_id = :musician AND mentors_id = :disrespected-by\n\n\n"
               (common/render-template
                (slurp templ-hugsql) (tbl/make-queries-keys crd9 entity3 p9 nil {}))))
        ;; A Musician one-to-one recursive relationship
        (is (= "-- :name get-musician-guru :? :1\n-- :doc returns guru associated with musician\nSELECT * FROM Musicians\nWHERE id = (SELECT musicians_id FROM Musicians__guru__r_oto WHERE musicians_id_r = :musician)\n\n-- :name get-musician-guru-reverse :? :1\n-- :doc hierarchical reverse operation of get-musician-guru\nSELECT * FROM Musicians\nWHERE id = (SELECT musicians_id_r FROM Musicians__guru__r_oto WHERE musicians_id = :musician)\n\n-- :name assoc-musician-guru! :!\n-- :doc associates musician with corresponding guru\nINSERT INTO Musicians__guru__r_oto (musicians_id, musicians_id_r)\nVALUES (:musician, :guru)\n\n-- :name assoc-musician-guru-reverse! :!\n-- :doc hierarchical reverse operation of assoc-musician-guru!\nINSERT INTO Musicians__guru__r_oto (musicians_id, musicians_id_r)\nVALUES (:guru, :musician)\n\n-- :name dissoc-musician-guru! :!\n-- :doc dissociates musician from corresponding guru\nDELETE FROM Musicians__guru__r_oto\nWHERE musicians_id = :musician AND musicians_id_r = :guru\n\n-- :name dissoc-musician-guru-reverse!\n-- :doc hierarchical reverse operation of dissoc-musician-guru!\nDELETE FROM Musicians__guru__r_oto\nWHERE musicians_id_r = :musician AND musicians_id = :guru\n\n\n"
               (common/render-template
                (slurp templ-hugsql) (tbl/make-queries-keys crd5 entity0 p5 nil {}))))
        ;; A Band many-to-many recursive relationship
        (is (= "-- :name get-band-influenced :? :*\n-- :doc returns influenced associated with band\nSELECT * FROM Bands\nWHERE id IN (SELECT bands_id FROM Bands__influenced__r_mtm WHERE bands_id_r = :band)\n\n-- :name get-band-influenced-reverse :? :*\n-- :doc hierarchical reverse operation of get-band-influenced\nSELECT * FROM Bands\nWHERE id IN (SELECT bands_id_r FROM Bands__influenced__r_mtm WHERE bands_id = :band)\n\n-- :name assoc-band-influenced! :!\n-- :doc associates band with corresponding influenced\nINSERT INTO Bands__influenced__r_mtm (bands_id, bands_id_r)\nVALUES \n/*~\n(let [single (:band params)]\n    (clojure.string/join\n        \", \" (for [m (:influenced params)]\"(\" single \", \" m \")\")))\n~*/\n\n-- :name assoc-band-influenced-reverse! :!\n-- :doc hierarchical reverse operation of assoc-band-influenced!\nINSERT INTO Bands__influenced__r_mtm (bands_id_r, bands_id)\nVALUES \n/*~\n(let [single (:influenced params)]\n    (clojure.string/join\n        \", \" (for [m (:band params)]\"(\" single \", \" m \")\")))\n~*/\n\n-- :name dissoc-band-influenced! :!\n-- :doc dissociates band from corresponding influenced\nDELETE FROM Bands__influenced__r_mtm\nWHERE bands_id = :band AND bands_id_r IN :tuple:influenced\n\n-- :name dissoc-band-influenced-reverse!\n-- :doc hierarchical reverse operation of dissoc-band-influenced!\nDELETE FROM Bands__influenced__r_mtm\nWHERE bands_id_r = :band AND bands_id IN :tuple:influenced\n\n-- :name dissoc-all-band-influenced! :!\n-- :doc dissociates all band from corresponding influenced\nDELETE FROM Bands__influenced__r_mtm\nWHERE bands_id = :band\n\n-- :name dissoc-all-band-influenced-reverse! :!\n-- :doc dissociates all band from corresponding influenced\nDELETE FROM Bands__influenced__r_mtm\nWHERE bands_id_r = :band\n\n\n"
               (common/render-template
                (slurp templ-hugsql) (tbl/make-queries-keys crd6 entity1 p6 nil {}))))
        ;; A simple collection relationship
        (is (= "-- :name get-musician-honors :? :*\n-- :doc returns honors associated with musician\nSELECT * FROM Musicians__honors__smpl_coll\nWHERE musicians_id = :musician\n\n-- :name assoc-musician-honors! :!\n-- :doc associates musician with corresponding honors\nINSERT INTO Musicians__honors__smpl_coll (musicians_id, honors)\nVALUES \n/*~\n(let [single (:musician params)]\n    (clojure.string/join\n        \", \" (for [m (:honors params)]\"(\" single \", \" m \")\")))\n~*/\n\n-- :name dissoc-musician-honors! :!\n-- :doc dissociates honors from musician\nDELETE FROM Musicians__honors__smpl_coll\nWHERE musicians_id = :musician AND honors IN :tuple:honors\n\n-- :name dissoc-all-musician-honors! :!\n-- :doc dissociates all honors from musician\nDELETE FROM Musicians__honors__smpl_coll\nWHERE musicians_id = :musician\n\n\n"
               (common/render-template
                (slurp templ-hugsql) (tbl/make-queries-keys crd4 entity0 p4 nil {})))))
       (eval-in-environment
        :mysql
        (is (= {} (tbl/make-create-tbl-keys crd2 entity1 p2 nil {})))
        (is (= {} (tbl/make-create-tbl-keys crd3 entity0 p3 nil {})))
        (is (= {:create-tables
                [{:ad-entity-plural "Musicians__honors__smpl_coll"
                  :ad-props-create-table
                  "(id INTEGER AUTO_INCREMENT PRIMARY KEY,\n musicians_id INTEGER REFERENCES Musicians(id),\n honors VARCHAR(30))"}]}
               (tbl/make-create-tbl-keys crd4 entity0 p4 nil {})))
        (is (= {:create-tables
                [{:ad-entity-plural "Musicians__guru__r_oto"
                  :ad-props-create-table
                  "(id INTEGER AUTO_INCREMENT PRIMARY KEY,\n musicians_id INTEGER REFERENCES Musicians(id),\n musicians_id_r INTEGER REFERENCES Musicians(id),\n UNIQUE(musicians_id, musicians_id_r))"}]}
               (tbl/make-create-tbl-keys crd5 entity0 p5 nil {})))
        (is (= {:create-tables
                [{:ad-entity-plural "Musicians__influenced__r_mtm" ,
                  :ad-props-create-table
                  "(id INTEGER AUTO_INCREMENT PRIMARY KEY,\n musicians_id INTEGER REFERENCES Musicians(id),\n musicians_id_r INTEGER REFERENCES Musicians(id))"}]}
               (tbl/make-create-tbl-keys crd6 entity0 p6 nil {})))
        ;; make-alter-tbl-keys test
        (is (= {:alter-tables [{:table           "Musicians"
                                :fk-name         "FK__Musicians__Bands__band"
                                :on              "band"
                                :to-table        "Bands"
                                :drop-constraint "FOREIGN KEY"}]}
               (tbl/make-alter-tbl-keys crd8 entity1 p8 nil {})
               (tbl/make-alter-tbl-keys crd0 entity0 p0 nil {})))
        (is (= {:alter-tables [{:table           "Musicians"
                                :fk-name         "FK__Musicians__Bands__dream_band"
                                :on              "dream_band"
                                :to-table        "Bands"
                                :drop-constraint "FOREIGN KEY"}]}
               (tbl/make-alter-tbl-keys crd3 entity0 p3 nil {})
               (tbl/make-alter-tbl-keys crd2 entity1 p2 nil {})))
        (is (= {} (tbl/make-alter-tbl-keys crd7 entity2 p7 nil {})))
        (is (= {} (tbl/make-alter-tbl-keys crd1 entity0 p1 nil {})))
        (is (= {} (tbl/make-alter-tbl-keys crd4 entity0 p4 nil {})))
        (is (= {} (tbl/make-alter-tbl-keys crd5 entity0 p5 nil {})))
        (is (= {} (tbl/make-alter-tbl-keys crd6 entity0 p6 nil {}))))))))

(deftest build-additional-templates-keys-test
  (testing "Returned keys which are used to fill additional tables,
            alter tables and additional queries templates."
    (let [results (:build-additional-templates-keys-test res/results)]
      (eval-in-program-model-context
       no-entities-edge-case
       (let [ents      (api/all-entities)
             db-t      :postgres
             ent-props (map #(second
                              (tbl/build-db-create-table-string %1 %2 db-t true
                                                                nil))
                            ents (map #(api/entity-properties %) ents))]
         (is (= {} (tbl/build-additional-templates-keys ent-props nil)))))
      (eval-in-program-model-context
       no-entities-no-about-edge-case
       (let [ents      (api/all-entities)
             db-t      :postgres
             ent-props (map #(second
                              (tbl/build-db-create-table-string %1 %2 db-t true
                                                                nil))
                            ents (map #(api/entity-properties %) ents))]
         (is (= {} (tbl/build-additional-templates-keys ent-props nil)))))
      ;; super-simple program
      (eval-in-program-model-context
       entity-have-no-properties
       (let [ents      (api/all-entities)
             db-t      :postgres
             ent-props (map #(second
                              (tbl/build-db-create-table-string %1 %2 db-t true
                                                                nil))
                            ents (map #(api/entity-properties %) ents))]
         (is (= (results 0) (tbl/build-additional-templates-keys ent-props nil)))))
      ;; :alter-tables and :queries are present in result?
      (eval-in-program-model-context
       custom-property-datatype
       (let [ents      (api/all-entities)
             db-t      :postgres
             ent-props (map #(second
                              (tbl/build-db-create-table-string %1 %2 db-t true
                                                                nil))
                            ents (map #(api/entity-properties %) ents))
             template-keys
             (tbl/build-additional-templates-keys ent-props nil)]
         (is (and (contains? template-keys :alter-tables)
                  (contains? template-keys :queries))))))))

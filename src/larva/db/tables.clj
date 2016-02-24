(ns larva.db.tables
  "Handles generation of template keys for additional tables generated from
  relationships between basic schema tables as well as their mutual
  constraints."
  (:require [clojure.string :as cs]
            [larva
             [program-api :as api]
             [program-api-schemes :as sch :refer [APIProperties]]
             [utils :as utils]]
            [larva.db
             [stuff :refer [database-grammar]]
             [utils :refer :all]]
            [schema.core :as s]))

(s/def DBStringRefs
  [(s/one s/Str "db-string") (s/optional {s/Str APIProperties} "net-refs")])

(s/def CreateTableMap
  (:create-tables [{:ad-entity-plural s/Str :ad-props-create-table s/Str}]))

(s/def DropMap
  {:drops [{:ad-entity-plural s/Str}]})

(s/def AlterMap
  {})

(s/def QueryMap
  {})

(s/def KeysMap
  {:create-tbl [CreateTableMap] :drop  [DropMap]
   :query      [QueryMap]       :alter [AlterMap]})

(defn- make-id-column-name [entity & recursive]
  (str (drill-out-name-for-db entity) "_id" (if recursive "_r")))

(defn- get-cardinality-keyword
  "Works only for those cardinalities which are originated from property
  that represents reference."
  [cardinality]
  (cond (or (nil? cardinality) (= {} cardinality)) :simple-collection
        (contains? cardinality :many-to-many)      :many-to-many
        (contains? cardinality :one-to-one)        :one-to-one
        (contains? cardinality :many-to-one)       :many-to-one
        (contains? cardinality :one-to-many)       :one-to-many))

(defn- infer-property-data-type
  "Returns a vector consisted of string to be placed as data type of table column
  if that column is needed and indicator that shows if it represents a reference."
  [prop-type-key cardinality db-types]
  (let [crd (get-cardinality-keyword cardinality)]
    (cond (utils/valid? sch/APIReferenceToSingleEntity prop-type-key)
          (if (contains? cardinality :recursive) [false true]
              [(:num db-types) true])
          (utils/valid? sch/APICollectionWithReference prop-type-key)
          (if (or (= crd :many-to-one) (contains? cardinality :recursive))
            [false true] [(:num db-types) true])
          (utils/valid? sch/APICollection prop-type-key)
          [false true]
          (utils/valid? sch/APISimpleDataType prop-type-key)
          [(get db-types prop-type-key) false]
          (utils/valid? sch/APICustomDataType prop-type-key)
          [prop-type-key false])))

(s/defn ^:always-validate build-db-create-table-string :- DBStringRefs
  "Returns a string to be placed in CREATE TABLE SQL statement and a vector of
  properties that are representing any kind of references mapped to
  corresponding entity."
  [entity :- s/Str properties db-type force args]
  (let [[_ db-types]
        (first (make-db-data-types-config :db-type db-type :force force))]
    (loop [props        properties
           props-w-refs {entity []}
           strings      [(str "id " (:id db-types) " "
                              (:prim-key (db-type database-grammar)))]]
      (if (not-empty props)
        (let [p         (nth props 0) t (:type p)
              crd       (if args (api/property-reference entity p args)
                            (api/property-reference entity p))
              [type rf] (infer-property-data-type t crd db-types)]
          (recur
           (rest props)
           (if rf {entity (conj (get props-w-refs entity) p)} props-w-refs)
           (if type (conj strings (str (drill-out-name-for-db (:name p)) " " type))
               strings)))
        [(str "(" (cs/join (str "," (System/lineSeparator) " ") strings) ")")
         props-w-refs]))))

(defn- build-additional-tbl-create-tbl-string
  [cardinality entity property args]
  (let [crd                (get-cardinality-keyword cardinality)
        [db-type db-types] (first (make-db-data-types-config))
        recursive          (contains? cardinality :recursive)
        uniq               (if (= crd :one-to-one) true)
        non-recursive-columns
        #(let [tbl1 (build-db-table-name entity args)
               tbl2 (build-db-table-name (% cardinality) args)]
           ((->> database-grammar db-type :referential-table-columns) db-types
            [(make-id-column-name tbl1) tbl1 "id" uniq]
            [(make-id-column-name tbl1) tbl2 "id" uniq]))
        recursive-columns
        #(let [tbl (build-db-table-name entity args)]
           ((->> database-grammar db-type :referential-table-columns) db-types
            [(make-id-column-name tbl) tbl "id" uniq]
            [(make-id-column-name tbl true) tbl "id" uniq]))]
    (if (not recursive)
      (case crd
        :many-to-many (non-recursive-columns :many-to-many)
        :one-to-one   (non-recursive-columns :one-to-one)
        :simple-collection
        (let [tbl (build-db-table-name entity)]
          ((->> database-grammar db-type :referential-table-columns) db-types
           [(make-id-column-name tbl) tbl "id"
            (drill-out-name-for-db (:name property))
            (get-in property [:type :coll])])))
      (recursive-columns))))

(defn- build-additional-tbl-name
  "Building name of an additional table which is relation by-product."
  [inferred-card entity property args]
  (let [crd       (get-cardinality-keyword inferred-card)
        recursive (contains? inferred-card :recursive)
        not-recursive-table-name-base
        #(str (build-db-table-name entity args) "__"
              (drill-out-name-for-db (:name property)) "__"
              (build-db-table-name (%1 inferred-card) args) "__"
              (drill-out-name-for-db (:back-property inferred-card)) %2)
        recursive-table-name-base
        #(str (build-db-table-name entity) "__"
              (drill-out-name-for-db (:back-property inferred-card)) %)]
    (if (not recursive)
      (case crd
        :many-to-many (not-recursive-table-name-base :many-to-many "__mtm")
        :one-to-one   (not-recursive-table-name-base :one-to-one "__oto")
        :simple-collection
        (str (build-db-table-name entity) "__"
             (drill-out-name-for-db (:name property))
             "__smpl_coll"))
      (case crd
        :one-to-one   (recursive-table-name-base "__r_oto")
        :many-to-many (recursive-table-name-base "__r_mtm")))))

(s/defn make-create-tbl-keys :- CreateTableMap
  "Make templates keys originated from one-to-one, many-to-many, one-to-many
   (when model expresses a simple collection) and recursive relations between
   entities."
  [cardinality entity property args keys-map]
  (let [crd       (get-cardinality-keyword cardinality)
        recursive (contains? cardinality :recursive)]
    (if (or (#{:many-to-many :one-to-one :simple-collection} crd)
            recursive)
      (merge-with
       concat keys-map
       {:create-tables [{:ad-entity-plural
                         (build-additional-tbl-name cardinality entity property
                                                    args)
                         :ad-props-create-table
                         (build-additional-tbl-create-tbl-string
                          cardinality entity property args)}]})
      keys-map)))

(s/defn make-drop-tbl-keys :- DropMap
  [inferred-card entity property args keys-map]
  (let [crd (get-cardinality-keyword inferred-card)]
    (if (#{:one-to-one :many-to-many :simple-collection} crd)
      (merge-with
       concat keys-map
       {:drops [{:ad-entity-plural
                 (build-additional-tbl-name inferred-card entity property args)}]})
      keys-map)))

(s/defn make-alter-tbl-keys :- AlterMap
  [inferred-card entity property args keys-map]
  )

(defn- get-corresponding-made-item [made-item made]
  (if-not (= :simple-collection (first made-item))
    (let [reverse-crd #(let [crd (first %)]
                         (case crd
                           :one-to-many :many-to-one
                           :many-to-one :one-to-many
                           crd))]
      (some #(= (made-item [(reverse-crd %) {:src  (:dest (second %))
                                             :dest (:src (second %))}]))
            (filter #(not (= :simple-collection (first %))) made)))))

(defn- make-keys
  "Building all the templates keys originated from relations between entities."
  [inferred-card entity property made-item made args]
  (if (not (get-corresponding-made-item made-item made))
    (let [params [inferred-card entity property args]]
      (->> (apply make-create-tbl-keys (conj params {}))
           (conj params) (apply make-drop-tbl-keys)
           (conj params) (apply make-alter-tbl-keys)
           ;; (apply make-queries-keys)
           ))))

(defn- form-already-made-item [inferred-cardinality entity property]
  (let [crd (get-cardinality-keyword inferred-cardinality)]
    [crd {:src  [entity (:name property)]
          :dest [(crd inferred-cardinality) (:back-perty inferred-cardinality)]}]))

(s/defn ^:always-validate build-additional-templates-keys
  "Building keys aimed to fulfill templates that create up and down migrations
   for relation-consequential tables, corresponding alters and queries."
  [ent-refs :- [{s/Str APIProperties}] args]
  (loop [er ent-refs made [] template-keys {}]
    (if (not-empty er)
      (let [[entity properties] (first (first er))
            [made-untll-now t-keys]
            (loop [props properties made [] t-keys []]
              (if (not-empty props)
                (let [p         (first props)
                      inferred-card
                      (or
                       (not-empty (if args (api/property-reference entity p args)
                                      (api/property-reference entity p))))
                      made-item (form-already-made-item inferred-card entity p)]
                  (recur (rest props) (conj made made-item)
                         (merge-with
                          concat
                          t-keys (make-keys inferred-card entity p made-item
                                            made args))))
                [made t-keys]))]
        (recur (rest er) (conj made made-untll-now)
               (merge-with concat template-keys t-keys)))
      template-keys)))

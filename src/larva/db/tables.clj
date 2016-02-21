(ns larva.db.tables
  "Handles generation of template keys for additional tables generated from
  relationships between basic schema tables."
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
  {:ad-entity-plural s/Str :ad-props-create-table s/Str})

(s/def DropMap
  {:ad-entity-plural s/Str})

(s/def QueryMap
  {})

(s/def AlterMap
  {})

(s/def KeysMap
  {:create-tbl [CreateTableMap] :drop  [DropMap]
   :query      [QueryMap]       :alter [AlterMap]})

(defn- make-id-column-name [entity]
  (str (drill-out-name-for-db entity) "_id"))

(defn- get-cardinality-keyword [cardinality]
  (cond (nil? cardinality)                    :simple-collection
        (contains? cardinality :many-to-many) :many-to-many
        (contains? cardinality :one-to-one)   :one-to-one
        (contains? cardinality :many-to-one)  :many-to-one
        (contains? cardinality :one-to-many)  :one-to-many))

(defn- infer-property-data-type
  "Returns a vector consisted of string to be placed as data type of table column
  if that column is needed and indicator that shows if it represents a reference."
  [prop-type-key cardinality db-types]
  (let [crd (get-cardinality-keyword cardinality)]
    (cond (utils/valid? sch/APIReferenceToSingleEntity prop-type-key)
          [(:num db-types) true]
          (utils/valid? sch/APICollectionWithReference prop-type-key)
          (if (= crd :many-to-one) [false true] [(:num db-types) true])
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
  (let [db-types (make-db-data-types-config :db-type db-type :force force)]
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
        [(str "(" (cs/join (str "," (System/lineSeparator) " ") strings)")")
         props-w-refs]))))

(defn- build-additional-tbl-create-tbl-string
  [cardinality entity args db-type]
  (let [crd      (get-cardinality-keyword cardinality)
        db-types (make-db-data-types-config :spec args :db-type db-type)
        uniq     (case crd :many-to-many false :one-to-one true)]
    ((->> database-grammar db-type :referential-table-columns) db-types
     [(make-id-column-name entity) (build-db-table-name entity args) "id"
      uniq]
     [(make-id-column-name (:many-to-many cardinality))
      (build-db-table-name (:many-to-many cardinality) args) "id" uniq])))

(s/defn ^:private make-create-tbl-keys :- CreateTableMap
  [cardinality entity property args db-type keys-map]
  (let [crd (get-cardinality-keyword cardinality)]
    (if (or (= crd :many-to-many) (= crd :one-to-one))
      {:ad-entity-plural (build-db-table-name entity args)
       :ad-props-create-table
       (build-additional-tbl-create-tbl-string cardinality entity args db-type)}
      keys-map)))

(defn- build-tbl-name
  "Building name of an additional table which is relation by-product."
  [inferred-card entity property args]
  (let [crd (get-cardinality-keyword inferred-card)]
    (case crd
      :many-to-many
      (str (build-db-table-name entity args)
           "_" (build-db-table-name (:many-to-many inferred-card) args)
           "_mtm")
      :one-to-one
      (str (build-db-table-name entity args true)
           "_" (build-db-table-name (:one-to-one inferred-card) args true)
           "_oto"))))

(defn- form-already-made-item [inferred-cardinality entity property]
  (let [crd (get-cardinality-keyword inferred-cardinality)]
    [crd {:src  [entity (:name property)]
          :dest [(crd inferred-cardinality) (:back-perty inferred-cardinality)]}]))

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

(s/defn ^:private make-drop-tbl-keys :- DropMap
  [inferred-card entity property args _ keys-map]
  (let [crd (get-cardinality-keyword inferred-card)]
    (if (or (= crd :many-to-many) (= crd :one-to-one))
      (merge-with
       concat keys-map
       {:drops [{:ad-entity-plural
                 (build-tbl-name inferred-card entity property args)}]})
      keys-map)))


(defn- make-keys [inferred-card entity property made-item made args db-type]
  (if (not (get-corresponding-made-item made-item made))
    (let [params [inferred-card entity property args db-type]]
      (->> (apply make-create-tbl-keys (conj params {}))
           (conj params)
           (apply make-drop-tbl-keys)
           ;; (apply make-alter-tbl-keys)
           ;; (apply make-queries-keys)
           ))))

(s/defn ^:always-validate build-additional-templates-keys
  "Building keys aimed to fulfill templates that create up and down migrations
   for relation-consequential tables, corresponding alters and queries."
  [ent-refs :- [{s/Str APIProperties}] db-type args]
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
                          t-keys (make-keys inferred-card entity
                                            p made-item made args db-type))))
                [made t-keys]))]
        (recur (rest er) (conj made made-untll-now)
               (merge-with concat template-keys t-keys)))
      template-keys)))

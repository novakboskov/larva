(ns larva.db.tables
  "Handles generation of template keys for additional tables generated from
  relationships between basic schema tables."
  (:require [clojure.string :as cs]
            [larva
             [program-api :as api]
             [program-api-schemes :refer [APIProperties]]]
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

(defn- get-cardinality-keyword [cardinality]
  (cond (nil? cardinality)                    :simple-collection
        (contains? cardinality :many-to-many) :many-to-many
        (contains? cardinality :one-to-one)   :one-to-one
        (contains? cardinality :many-to-one)  :many-to-one
        (contains? cardinality :one-to-many)  :one-to-many))

(s/defn ^:always-validate build-db-create-table-string :- DBStringRefs
  "Returns a string to be placed in CREATE TABLE SQL statement and a vector of
  properties that are representing any kind of references mapped to
  corresponding entity."
  [entity :- s/Str properties db-type force]
  (let [db-types (make-db-data-types-config :db-type db-type :force force)]
    (loop [props        properties
           props-w-refs {entity []}
           strings      [(str "id " (:id db-types) " "
                              (:prim-key (db-type database-grammar)))]]
      (if (not-empty props)
        (let [p         (nth props 0) t (:type p)
              [type rf] (infer-property-data-type t db-types)]
          (recur
           (rest props)
           (if rf {entity (conj (get props-w-refs entity) p)} props-w-refs)
           (if type (conj strings (str (drill-out-name-for-db (:name p)) " " type))
               strings)))
        [(str "(" (cs/join (str "," (System/lineSeparator) " ") strings)")")
         props-w-refs]))))

(defn- build-additional-db-create-tbl-string
  [cardinality entity args db-type]
  (let [db-types (make-db-data-types-config {:spec args :db-type db-type})
        crd      (get-cardinality-keyword cardinality)]
    (case crd
      :many-to-many
      [(str "id " (:id db-types) " " (:prim-key (db-type database-grammar)) ","
            (System/lineSeparator)
            ;; TODO:
            )])))

(s/defn make-create-tbl-keys-dispatch :- KeysMap
  [cardinality _ _ _ _ _]
  (get-cardinality-keyword cardinality))

(defmulti ^:private make-create-tbl-keys
  (fn [cardinality _ _ _ _ _] (get-cardinality-keyword cardinality)))

(defmethod make-create-tbl-keys :many-to-many
  [cardinality entity property args db-type keys-map]
  {:ad-entity-plural (build-db-table-name entity args)
   :ad-props-create-table
   (build-additional-db-create-tbl-string cardinality entity args db-type)})

(defmethod make-create-tbl-keys :one-to-one
  [cardinality entity property args db-type keys-map]
  )

(defmethod make-create-tbl-keys :many-to-one
  [cardinality entity property args db-type keys-map]
  )

(defmethod make-create-tbl-keys :one-to-many
  [cardinality entity property args db-type keys-map]
  )

(defmethod make-create-tbl-keys :simple-collection
  [cardinality entity property args db-type keys-map]
  )

(defmulti build-tbl-name
  (fn [inferred-card _ _ _] (get-cardinality-keyword inferred-card)))

(defmethod build-tbl-name :many-to-many
  [inferred-card entity property args]
  (str (build-db-table-name entity args)
       "_" (build-db-table-name (:many-to-many inferred-card) args)
       "_mtm"))

(defmethod build-tbl-name :one-to-one
  [inferred-card entity property args]
  (str (build-db-table-name entity args true)
       "_" (build-db-table-name (:one-to-one inferred-card) args true)
       "_oto"))

(defmethod build-tbl-name :one-to-many
  [inferred-card entity property args]
  (str (build-db-table-name entity args true)
       "_" (build-db-table-name (:one-to-many infer-db-type) args)
       "_otm"))

(defmethod build-tbl-name :many-to-one
  [inferred-card entity property args]
  (str (build-db-table-name entity args)
       "_" (build-db-table-name (:one-to-many infer-db-type) args true)
       "_mto"))

(defmethod build-tbl-name :simple-collection
  [inferred-card entity property args]
  (str (build-db-table-name entity args)
       "_" (drill-out-name-for-db (:name property)) "_scl"))

(defn- form-already-made-item [inferred-cardinality entity property]
  (let [crd (get-cardinality-keyword inferred-cardinality)]
    [crd {:src  [entity (:name property)]
          :dest [(crd inferred-cardinality) (:back-perty inferred-cardinality)]}]))

(defn- get-corresponding-made-item [made-item made]
  (let [reverse-crd #(let [crd (first %)]
                       (case crd
                         :one-to-many :many-to-one
                         :many-to-one :one-to-many
                         :else        crd))]
    (some #(= (made-item [(reverse-crd %) {:src  (:dest (second %))
                                           :dest (:src (second %))}])) made)))

(defn- make-drop-tbl-keys
  [inferred-card entity property keys-map args]
  (let [cardinality-key (get-cardinality-keyword inferred-card)
        make-drops-item #(merge-with concat keys-map
                                     {:drops [{:ad-entity-plural %}]})]
    (make-drops-item (build-tbl-name inferred-card entity property args))))

(defn- make-keys [inferred-card entity property made-item made args db-type]
  (if (not (get-corresponding-made-item made-item made))
    (let [params [inferred-card entity property args db-type]]
      (->> (apply make-create-tbl-keys (conj params {}))
           (conj params)
           (apply make-drop-tbl-keys)
           ;; (apply make-alter-tbl-keys)
           ;; (apply make-queries-keys)
           ))))

(s/defn ^:always-validate build-add-db-create-table-keys
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
                                      (api/property-reference entity p)))
                       {:simple-collection entity})
                      made-item (form-already-made-item
                                 inferred-card entity p)]
                  (recur (rest props)
                         (if inferred-card (conj made made-item) made)
                         (merge-with
                          concat
                          t-keys (make-keys inferred-card entity
                                            p made-item made args db-type))))
                [made t-keys]))]
        (recur (rest er) (conj made made-untll-now)
               (merge-with concat template-keys t-keys)))
      template-keys)))

(defn build-alter-tables-strings
  [ref-properties]
  ;; TODO:
  )

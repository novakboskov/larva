(ns larva.db.tables
  (:require [clojure.string :as cs]
            [larva
             [program-api :as api]
             [program-api-schemes :refer [APIProperties]]]
            [larva.db
             [stuff :refer [database-grammar]]
             [utils :as utils]]
            [schema.core :as s]))

(s/def DBStringRefs
  [(s/one s/Str "db-string") (s/optional {s/Str APIProperties} "net-refs")])

(s/defn ^:always-validate build-db-create-table-string :- DBStringRefs
  "Returns a string to be placed in CREATE TABLE SQL statement and a vector of
  properties that are representing any kind of references mapped to
  corresponding entity."
  [entity :- s/Str properties db-type force]
  (let [db-types (utils/make-db-data-types-config :db-type db-type :force force)]
    (loop [props        properties
           props-w-refs {entity []}
           strings      [(str "id " (:id db-types) " "
                              (:prim-key (db-type database-grammar)))]]
      (if (not-empty props)
        (let [p         (nth props 0) t (:type p)
              [type rf] (utils/infer-property-data-type t db-types)]
          (recur
           (rest props)
           (if rf {entity (conj (get props-w-refs entity) p)} props-w-refs)
           (if type (conj strings (str (utils/drill-out-name-for-db (:name p)) " " type))
               strings)))
        [(str "(" (cs/join (str "," (System/lineSeparator) " ") strings)")")
         props-w-refs]))))

(defn- get-cardinality-keyword [cardinality]
  (cond (nil? cardinality) :simple-collection
        (contains? cardinality :many-to-many) :many-to-many
        (contains? cardinality :one-to-one)   :one-to-one
        (contains? cardinality :many-to-one)  :many-to-one
        (contains? cardinality :one-to-many)  :one-to-many))

(defmulti ^:private make-create-tbl-keys
  (fn [cardinality _ _] (get-cardinality-keyword cardinality)))

(defmethod make-create-tbl-keys :many-to-many
  [cardinality properties made]
  )

(defmethod make-create-tbl-keys :one-to-one
  [cardinality properties made]
  )

(defmethod make-create-tbl-keys :many-to-one
  [cardinality properties made]
  )

(defmethod make-create-tbl-keys :one-to-many
  [cardinality properties made]
  )

(defmethod make-create-tbl-keys :simple-collection
  [cardinality properties made]
  )

(defn- form-already-made-item [inferred-cardinality entity property]
  (let [crd (get-cardinality-keyword inferred-cardinality)]
    [crd {:src  [entity (:name property)]
          :dest [(crd inferred-cardinality) (:back-perty inferred-cardinality)]}]))

(defn- make-keys [inferred-card p made]
  ;; TODO:
  ;; (let [params [inferred-card p made]
  ;;       (apply make-create-tbl-keys params)])
  )

(s/defn ^:always-validate build-add-db-create-table-keys
  [ent-refs :- [{s/Str APIProperties}] db-type args]
  (loop [er ent-refs made [] template-keys []]
    (if (not-empty er)
      (let [[entity properties] (first (first er))
            [made-untll-now t-keys]
            (loop [props properties made [] t-keys []]
              (if (not-empty props)
                (let [p (first props)
                      inferred-card
                      (not-empty (if args (api/property-reference entity p args)
                                     (api/property-reference entity p)))]
                  (recur (rest props)
                         (if inferred-card (conj made (form-already-made-item
                                                       inferred-card entity p))
                             made)
                         (conj t-keys (make-keys inferred-card p made))))
                [made t-keys]))]
        (recur (rest er) (conj made made-untll-now) (conj template-keys t-keys)))
      template-keys)))

(defn build-alter-tables-strings
  [ref-properties]
  ;; TODO:
  )

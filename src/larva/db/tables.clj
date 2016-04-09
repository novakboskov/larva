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
             [commons :refer :all]
             [queries :refer [queries]]
             [stuff :refer [database-grammar]]
             [utils :refer :all]]
            [schema.core :as s]))

(s/def DBStringRefs
  [(s/one s/Str "db-string") (s/optional {s/Str APIProperties} "net-refs")])

(s/def CreateTableMap
  {:ad-entity-plural s/Str :ad-props-create-table s/Str})

(s/def AlterMap
  {:table s/Str :fk-name s/Str :on s/Str :to-table s/Str})

(s/def QueryMap
  {:ent                            s/Str  :prop                        s/Str
   :f-tbl                          s/Str  :f-id                        s/Str
   (s/optional-key :sign)          s/Str  (s/optional-key :update)     s/Bool
   (s/optional-key :no-nest)       s/Bool (s/optional-key :s-id)       s/Str
   (s/optional-key :s-tbl)         s/Str  (s/optional-key :t-id)       s/Str
   (s/optional-key :assoc)         s/Bool (s/optional-key :dissoc)     s/Bool
   (s/optional-key :update-where)  s/Str  (s/optional-key :f-id-val)   s/Str
   (s/optional-key :sel-multi)     s/Bool (s/optional-key :dissoc-all) s/Bool
   (s/optional-key :insert-values) s/Str  (s/optional-key :and-single) s/Bool
   (s/optional-key :reverse-doc)   s/Bool (s/optional-key :assoc-rev)  s/Bool
   (s/optional-key :dissoc-rev)    s/Bool (s/optional-key :get-rev)    s/Bool
   (s/optional-key :name-rev)      s/Bool})

(s/def TableKeys
  {(s/optional-key :create-tables) [CreateTableMap]})

(s/def AlterKeys
  {(s/optional-key :create-tables) [CreateTableMap]
   (s/optional-key :alter-tables)  [AlterMap]})

(s/def QueryKeys
  {(s/optional-key :create-tables) [CreateTableMap]
   (s/optional-key :alter-tables)  [AlterMap]
   (s/optional-key :queries)       [QueryMap]})

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
        (let [tbl (build-db-table-name entity args)]
          ((->> database-grammar db-type :referential-table-columns) db-types
           [(make-id-column-name tbl) tbl "id"
            (drill-out-name-for-db (:name property))
            (get-in property [:type :coll])])))
      (recursive-columns))))

(s/defn ^:always-validate make-create-tbl-keys :- TableKeys
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

(s/defn ^:always-validate make-alter-tbl-keys :- AlterKeys
  [cardinality entity property args keys-map :- TableKeys]
  (if-let [crd (#{:many-to-one :one-to-many}
                (get-cardinality-keyword cardinality))]
    (let [merge-keys     #(merge-with concat keys-map {:alter-tables [%]})
          this-tbl       (build-db-table-name entity args)
          referenced-tbl (build-db-table-name (crd cardinality) args)
          prop-name      (:name property)]
      (case crd
        :one-to-many
        (merge-keys {:table    this-tbl
                     :fk-name  (build-foreign-key-name referenced-tbl this-tbl
                                                       prop-name)
                     :on       (drill-out-name-for-db prop-name)
                     :to-table referenced-tbl})
        :many-to-one
        (merge-keys {:table    referenced-tbl
                     :fk-name  (build-foreign-key-name
                                this-tbl referenced-tbl
                                (:back-property cardinality))
                     :on       (drill-out-name-for-db
                                (:back-property cardinality))
                     :to-table this-tbl})))
    keys-map))

(defn- do-qs-functions-calls
  "Performs functions that return queries keys maps"
  [q-get q-assoc q-dissoc queries-key]
  (concat ((queries-key q-get))
          ((queries-key q-assoc))
          ((queries-key q-dissoc))))

(s/defn ^:always-validate make-queries-keys :- QueryKeys
  [cardinality entity property args keys-map :- AlterKeys]
  (let [crd        (get-cardinality-keyword cardinality)
        recursive  (contains? cardinality :recursive)
        queries    (queries cardinality entity property args crd recursive)
        q-get      (:get queries)
        q-assoc    (:assoc queries)
        q-dissoc   (:dissoc queries)
        merge-keys #(merge-with
                     concat keys-map
                     {:queries (do-qs-functions-calls q-get q-assoc q-dissoc %)})]
    (if (not recursive)
      (case crd
        :one-to-many                (merge-keys :one-side-qs)
        :many-to-one                (merge-keys :many-side-qs)
        (:many-to-many :one-to-one) (merge-keys :oto&mtm-qs)
        :simple-collection          (merge-keys :smpl-coll-qs))
      (merge-keys :recursive-qs))))

(defn- get-corresponding-made-item [made-item made]
  (if-not (= :simple-collection (first made-item))
    (let [reverse-crd #(let [crd (first %)]
                         (case crd
                           :one-to-many :many-to-one
                           :many-to-one :one-to-many
                           crd))]
      (some #(= made-item [(reverse-crd %) {:src  (:dest (second %))
                                            :dest (:src (second %))}])
            (filter #(not (= :simple-collection (first %))) made)))))

(defn- make-keys
  "Building all the templates keys originated from relations between entities."
  [inferred-card entity property made-item made args]
  (if (not (get-corresponding-made-item made-item made))
    (let [params [inferred-card entity property args]]
      (->> (apply make-create-tbl-keys (conj params {}))
           (conj params) (apply make-alter-tbl-keys)
           (conj params) (apply make-queries-keys)))))

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
            (loop [props properties made [] t-keys {}]
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

(ns larva.db.commons
  (:require [larva.db.utils :refer :all]))

(defn make-id-column-name [entity & recursive]
  (str (drill-out-name-for-db entity) "_id" (if recursive "_r")))

(defn get-cardinality-keyword
  "Works only for those cardinalities which are originated from property
  that represents reference."
  [cardinality]
  (cond (or (nil? cardinality) (= {} cardinality)) :simple-collection
        (contains? cardinality :many-to-many)      :many-to-many
        (contains? cardinality :one-to-one)        :one-to-one
        (contains? cardinality :many-to-one)       :many-to-one
        (contains? cardinality :one-to-many)       :one-to-many))

(defn build-additional-tbl-name
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

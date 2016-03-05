(ns larva.db.queries
  (:require [larva.db.utils :refer :all]
            [larva.db.commons :refer :all]))

(defn- get-queries [cardinality entity property args crd recursive]
  {:one-side-select-q
   #(identity {:ent   (drill-out-name-for-clojure entity)
               :prop  (drill-out-name-for-clojure (:name property))
               :f-tbl (build-db-table-name (crd cardinality) args)
               :f-id  "id" :sign "="
               :s-id  (drill-out-name-for-db (:name property))
               :t-tbl (build-db-table-name entity args)
               :t-id  "id"})
   :many-side-select-q
   #(identity {:ent     (drill-out-name-for-clojure (crd cardinality))
               :prop    (drill-out-name-for-clojure (:back-property
                                                     cardinality))
               :f-tbl   (build-db-table-name entity args)
               :f-id    (drill-out-name-for-db (:name property)) :sign "="
               :no-nest true})
   :oto&mtm-select-qs
   #(identity [{:ent   (drill-out-name-for-clojure entity)
                :prop  (drill-out-name-for-clojure (:name property))
                :f-tbl (build-db-table-name (crd cardinality) args)
                :f-id  "id" :sign "IN"
                :s-id  (make-id-column-name (crd cardinality))
                :t-tbl (build-additional-tbl-name
                        cardinality entity property args)
                :t-id  (make-id-column-name entity)}
               {:ent   (drill-out-name-for-clojure (crd cardinality))
                :prop  (drill-out-name-for-clojure (:back-property
                                                    cardinality))
                :f-tbl (build-db-table-name entity)
                :f-id  "id" :sign "IN"
                :s-id  (make-id-column-name entity)
                :t-tbl (build-additional-tbl-name
                        cardinality entity property args)
                :t-id  (make-id-column-name (crd cardinality))}])
   :simpl-coll-select-q
   #(identity {:ent     (drill-out-name-for-clojure entity)
               :prop    (drill-out-name-for-clojure (:name property))
               :f-tbl   (build-additional-tbl-name
                         cardinality entity property args)
               :f-id    (make-id-column-name entity) :sign "="
               :no-nest true})
   :recursive_select-q
   #(identity {:ent   (drill-out-name-for-clojure entity)
               :prop  (drill-out-name-for-clojure (:name property))
               :f-tbl (build-db-table-name entity args)
               :f-id  "id" :sign (if % "IN" "=")
               :s-id  (make-id-column-name (:name property))
               :t-tbl (build-additional-tbl-name
                       cardinality entity property args)
               :t-id  (make-id-column-name (:name property) true)})})

(defn queries
  [cardinality entity property args crd recursive]
  {:get (get-queries cardinality entity property args crd recursive)})

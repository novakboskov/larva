(ns larva.db.queries
  (:require [larva.db.utils :refer :all]
            [larva.db.commons :refer :all]))

(defn- get-queries [cardinality entity property args crd recursive]
  {:one-side-q
   #(identity {:ent   (drill-out-name-for-clojure entity)
               :prop  (drill-out-name-for-clojure (:name property))
               :f-tbl (build-db-table-name (crd cardinality) args)
               :f-id  "id" :sign "="
               :s-id  (drill-out-name-for-db (:name property))
               :t-tbl (build-db-table-name entity args)
               :t-id  "id"})
   :many-side-q
   #(identity {:ent     (drill-out-name-for-clojure (crd cardinality))
               :prop    (drill-out-name-for-clojure (:back-property
                                                     cardinality))
               :f-tbl   (build-db-table-name entity args)
               :f-id    (drill-out-name-for-db (:name property)) :sign "="
               :no-nest true})
   :oto&mtm-qs
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
   :simpl-coll-q
   #(identity {:ent     (drill-out-name-for-clojure entity)
               :prop    (drill-out-name-for-clojure (:name property))
               :f-tbl   (build-additional-tbl-name
                         cardinality entity property args)
               :f-id    (make-id-column-name entity) :sign "="
               :no-nest true})
   :recursive-q
   #(identity {:ent   (drill-out-name-for-clojure entity)
               :prop  (drill-out-name-for-clojure (:name property))
               :f-tbl (build-db-table-name entity args)
               :f-id  "id" :sign (if % "IN" "=")
               :s-id  (make-id-column-name (:name property))
               :t-tbl (build-additional-tbl-name
                       cardinality entity property args)
               :t-id  (make-id-column-name (:name property) true)})})

(defn- assoc-queries [cardinality entity property args crd recursive]
  ;; TODO: Build look what happens here and, build dissoc keys and so...
  {:one-side-q
   #(identity (let [ent  (drill-out-name-for-clojure entity)
                    prop (drill-out-name-for-clojure (:name property))]
                {:assoc        true :update true
                 :ent          ent
                 :prop         prop
                 :f-tbl        (build-db-table-name entity args)
                 :f-id         (drill-out-name-for-db (:name property))
                 :f-id-val     prop
                 :s-id         "id"
                 :update-where (str "= :" ent)}))
   :many-side-q
   #(identity (let [ent (drill-out-name-for-clojure entity)]
                {:assoc        true :update true
                 :ent          ent
                 :prop         (drill-out-name-for-clojure (:name property))
                 :f-tbl        (build-db-table-name (crd cardinality) args)
                 :f-id         (drill-out-name-for-db (:back-property
                                                       cardinality))
                 :f-id-val     (drill-out-name-for-db (:back-property
                                                       cardinality))
                 :s-id         "id"
                 :update-where (str "= :tuple*:"
                                    (drill-out-name-for-clojure (:name property)))}))
   :oto&mtm-qs   ""
   :simpl-coll-q ""
   :recursive-q  ""})

(defn queries
  [cardinality entity property args crd recursive]
  {:get   (get-queries cardinality entity property args crd recursive)
   :assoc (assoc-queries cardinality entity property args crd recursive)})

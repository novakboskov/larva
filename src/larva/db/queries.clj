(ns larva.db.queries
  (:require [larva.db.utils :refer :all]
            [larva.db.commons :refer :all]))

(defn- get-queries [cardinality entity property args crd recursive]
  {:one-side-qs
   ;; Generate query keys when it proceed a property witch represents "one" side
   ;; of an one-to-many relationship
   #(identity [{:ent   (drill-out-name-for-clojure entity)
                :prop  (drill-out-name-for-clojure (:name property))
                :f-tbl (build-db-table-name (crd cardinality) args)
                :f-id  "id" :sign "="
                :s-id  (drill-out-name-for-db (:name property))
                :s-tbl (build-db-table-name entity args)
                :t-id  "id"}
               {:ent     (drill-out-name-for-clojure (crd cardinality))
                :prop    (drill-out-name-for-clojure (:back-property
                                                      cardinality))
                :f-tbl   (build-db-table-name entity args)
                :f-id    (drill-out-name-for-db (:name property)) :sign "="
                :no-nest true :sel-multi true}])
   :many-side-qs
   #(identity [{:ent   (drill-out-name-for-clojure (crd cardinality))
                :prop  (drill-out-name-for-clojure (:back-property
                                                    cardinality))
                :f-tbl (build-db-table-name entity args)
                :f-id  "id" :sign "="
                :s-id  (drill-out-name-for-db (:back-property
                                               cardinality))
                :s-tbl (build-db-table-name (crd cardinality) args)
                :t-id  "id"}
               {:ent   (drill-out-name-for-clojure entity)
                :prop  (drill-out-name-for-clojure (:name property))
                :f-tbl (build-db-table-name (crd cardinality) args)
                :f-id  (drill-out-name-for-db (:back-property
                                               cardinality))
                :sign  "=" :no-nest true :sel-multi true}])
   :oto&mtm-qs
   #(identity [{:ent   (drill-out-name-for-clojure entity)
                :prop  (drill-out-name-for-clojure (:name property))
                :f-tbl (build-db-table-name (crd cardinality) args)
                :f-id  "id" :sign (if (= % :many-to-many) "IN" "=")
                :s-id  (make-id-column-name (crd cardinality))
                :s-tbl (build-additional-tbl-name
                        cardinality entity property args)
                :t-id  (make-id-column-name entity)
                :sel-multi (true? (= % :many-to-many))}
               {:ent   (drill-out-name-for-clojure (crd cardinality))
                :prop  (drill-out-name-for-clojure (:back-property
                                                    cardinality))
                :f-tbl (build-db-table-name entity)
                :f-id  "id" :sign (if (= % :many-to-many) "IN" "=")
                :s-id  (make-id-column-name entity)
                :s-tbl (build-additional-tbl-name
                        cardinality entity property args)
                :t-id  (make-id-column-name (crd cardinality))
                :sel-multi (true? (= % :many-to-many))}])
   :simpl-coll-q
   #(identity {:ent     (drill-out-name-for-clojure entity)
               :prop    (drill-out-name-for-clojure (:name property))
               :f-tbl   (build-additional-tbl-name
                         cardinality entity property args)
               :f-id    (make-id-column-name entity) :sign "="
               :no-nest true :sel-multi true})
   :recursive-q
   #(identity {:ent   (drill-out-name-for-clojure entity)
               :prop  (drill-out-name-for-clojure (:name property))
               :f-tbl (build-db-table-name entity args)
               :f-id  "id" :sign (if (= % :many-to-many) "IN" "=")
               :s-id  (make-id-column-name entity)
               :s-tbl (build-additional-tbl-name
                       cardinality entity property args)
               :t-id  (make-id-column-name entity true)
               :sel-multi (true? (= % :many-to-many))})})

(defn- assoc-queries [cardinality entity property args crd recursive]
  ;; TODO: Look what happens here and, build dissoc keys and so...
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
   #(identity (let [ent (drill-out-name-for-clojure (crd cardinality))]
                {:assoc        true :update true
                 :ent          ent
                 :prop         (drill-out-name-for-clojure (:back-property
                                                            cardinality))
                 :f-tbl        (build-db-table-name entity args)
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

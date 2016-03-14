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
                :f-id    (drill-out-name-for-db (:name property))
                :sign    "="
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
   #(identity [{:ent       (drill-out-name-for-clojure entity)
                :prop      (drill-out-name-for-clojure (:name property))
                :f-tbl     (build-db-table-name (crd cardinality) args)
                :f-id      "id" :sign (if (= % :many-to-many) "IN" "=")
                :s-id      (make-id-column-name (crd cardinality))
                :s-tbl     (build-additional-tbl-name
                            cardinality entity property args)
                :t-id      (make-id-column-name entity)
                :sel-multi (true? (= % :many-to-many))}
               {:ent       (drill-out-name-for-clojure (crd cardinality))
                :prop      (drill-out-name-for-clojure (:back-property
                                                        cardinality))
                :f-tbl     (build-db-table-name entity)
                :f-id      "id" :sign (if (= % :many-to-many) "IN" "=")
                :s-id      (make-id-column-name entity)
                :s-tbl     (build-additional-tbl-name
                            cardinality entity property args)
                :t-id      (make-id-column-name (crd cardinality))
                :sel-multi (true? (= % :many-to-many))}])
   :simpl-coll-q
   #(identity {:ent     (drill-out-name-for-clojure entity)
               :prop    (drill-out-name-for-clojure (:name property))
               :f-tbl   (build-additional-tbl-name
                         cardinality entity property args)
               :f-id    (make-id-column-name entity) :sign      "="
               :no-nest true                         :sel-multi true})
   :recursive-q
   #(identity {:ent       (drill-out-name-for-clojure entity)
               :prop      (drill-out-name-for-clojure (:name property))
               :f-tbl     (build-db-table-name entity args)
               :f-id      "id" :sign (if (= % :many-to-many) "IN" "=")
               :s-id      (make-id-column-name entity)
               :s-tbl     (build-additional-tbl-name
                           cardinality entity property args)
               :t-id      (make-id-column-name entity true)
               :sel-multi (true? (= % :many-to-many))})})

(defn- assoc-queries [cardinality entity property args crd recursive]
  {:one-side-qs
   #(identity [(let [ent  (drill-out-name-for-clojure entity)
                     prop (drill-out-name-for-clojure (:name property))]
                 {:assoc        true :update true
                  :ent          ent
                  :prop         prop
                  :f-tbl        (build-db-table-name entity args)
                  :f-id         (drill-out-name-for-db (:name property))
                  :f-id-val     prop
                  :s-id         "id"
                  :update-where (str "= :" ent)})
               (let [prop (drill-out-name-for-clojure
                           (:back-property cardinality))]
                 {:assoc        true :update true
                  :ent          (drill-out-name-for-clojure (crd cardinality))
                  :prop         prop
                  :f-tbl        (build-db-table-name entity args)
                  :f-id         (drill-out-name-for-db (:name property))
                  :f-id-val     (drill-out-name-for-clojure (:name property))
                  :s-id         "id"
                  :update-where (str "IN :tuple:" prop)})])
   :many-side-qs
   #(identity [(let [ent (drill-out-name-for-clojure (crd cardinality))]
                 {:assoc        true :update true
                  :ent          ent
                  :prop         (drill-out-name-for-clojure (:back-property
                                                             cardinality))
                  :f-tbl        (build-db-table-name (crd cardinality) args)
                  :f-id         (drill-out-name-for-db (:back-property
                                                        cardinality))
                  :f-id-val     (drill-out-name-for-db (:back-property
                                                        cardinality))
                  :s-id         "id"
                  :update-where (str "= :" ent)})
               (let [prop (drill-out-name-for-clojure (:name property))]
                 {:assoc        true :update true
                  :ent          (drill-out-name-for-clojure entity)
                  :prop         prop
                  :f-tbl        (build-db-table-name (crd cardinality) args)
                  :f-id         (drill-out-name-for-db (:back-property
                                                        cardinality))
                  :f-id-val     (drill-out-name-for-db (:back-property
                                                        cardinality))
                  :s-id         "id"
                  :update-where (str "IN :tuple:" prop)})])
   :oto&mtm-qs
   #(identity [(let [ent  (drill-out-name-for-clojure entity)
                     prop (drill-out-name-for-clojure (:name property))]
                 {:assoc         true
                  :ent           ent :prop prop
                  :f-tbl         (build-additional-tbl-name
                                  cardinality entity property args)
                  :f-id          (make-id-column-name entity)
                  :s-id          (make-id-column-name (crd cardinality))
                  :insert-values (make-hugsql-values-string (str ":" ent)
                                                            (str ":" prop))})
               (let [ent  (drill-out-name-for-clojure (crd cardinality))
                     prop (drill-out-name-for-clojure (:back-property
                                                       cardinality))]
                 {:assoc         true
                  :ent           ent :prop prop
                  :f-tbl         (build-additional-tbl-name
                                  cardinality entity property args)
                  :f-id          (make-id-column-name (crd cardinality))
                  :s-id          (make-id-column-name entity)
                  :insert-values (make-hugsql-values-string (str ":" ent)
                                                            (str ":" prop))})])
   :simpl-coll-q ""
   :recursive-q  ""})

(defn assoc->dissoc-queries
  [side params]
  (fn [] (let [side-key  (case side
                           :one-side :one-side-qs
                           :oto&mtm  :oto&mtm-qs
                           :many-side-qs)
               qs        ((side-key (apply assoc-queries params)))
               make-dissoc-all-q
               (fn [from update-id]
                 [(-> from
                      (dissoc :assoc :f-id-val :s-id :update-where)
                      (assoc :dissoc-all true
                             :s-id update-id
                             :update-where (str "= :" update-id)))])]
           (concat (mapv #(assoc (dissoc % :assoc :f-id-val) :dissoc true) qs)
                   ;; dissoc-all queries
                   (make-dissoc-all-q (second qs) (:prop (first qs)))
                   (if (= :oto&mtm-qs side-key)
                     (make-dissoc-all-q (first qs) (:prop (second qs))))))))

(defn- dissoc-queries
  [& [cardinality entity property args crd recursive :as p]]
  {:one-side-qs  (assoc->dissoc-queries :one-side p)
   :many-side-qs (assoc->dissoc-queries :many-side p)
   :oto&mtm-qs   (assoc->dissoc-queries :oto&mtm p)
   :simpl-coll-q ""
   :recursive-q  ""})

(defn queries
  [cardinality entity property args crd recursive]
  {:get    (get-queries cardinality entity property args crd recursive)
   :assoc  (assoc-queries cardinality entity property args crd recursive)
   :dissoc (dissoc-queries cardinality entity property args crd recursive)})

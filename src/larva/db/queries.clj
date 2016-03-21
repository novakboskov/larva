(ns larva.db.queries
  (:require [larva.db.utils :refer :all]
            [larva.db.commons :refer :all]))

(defmulti get-get-query-function-for (fn [for _ _ _ _ _ _] for))

(defmethod get-get-query-function-for :one-side-qs
  [for cardinality entity property args crd recursive]
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
               :no-nest true :sel-multi true}]))

(defmethod get-get-query-function-for :many-side-qs
  [for cardinality entity property args crd recursive]
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
               :sign  "=" :no-nest true :sel-multi true}]))

(defmethod get-get-query-function-for :oto&mtm-qs
  [for cardinality entity property args crd recursive]
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
               :sel-multi (true? (= % :many-to-many))}]))

(defmethod get-get-query-function-for :simpl-coll-q
  [for cardinality entity property args crd recursive]
  #(identity [{:ent     (drill-out-name-for-clojure entity)
               :prop    (drill-out-name-for-clojure (:name property))
               :f-tbl   (build-additional-tbl-name
                         cardinality entity property args)
               :f-id    (make-id-column-name entity) :sign      "="
               :no-nest true                         :sel-multi true}]))

(defmethod get-get-query-function-for :recursive-q
  [for cardinality entity property args crd recursive]
  #(identity {:ent       (drill-out-name-for-clojure entity)
              :prop      (drill-out-name-for-clojure (:name property))
              :f-tbl     (build-db-table-name entity args)
              :f-id      "id" :sign (if (= % :many-to-many) "IN" "=")
              :s-id      (make-id-column-name entity)
              :s-tbl     (build-additional-tbl-name
                          cardinality entity property args)
              :t-id      (make-id-column-name entity true)
              :sel-multi (true? (= % :many-to-many))}))

(defn- get-queries [cardinality entity property args crd recursive]
  {:one-side-qs
   ;; Generate query keys when it proceed a property witch represents "one" side
   ;; of an one-to-many relationship
   (get-get-query-function-for :one-side-qs cardinality entity
                               property args crd recursive)
   :many-side-qs
   (get-get-query-function-for :many-side-qs  cardinality entity
                               property args crd recursive)
   :oto&mtm-qs
   (get-get-query-function-for :oto&mtm-qs  cardinality entity
                               property args crd recursive)
   :simpl-coll-q
   (get-get-query-function-for :simpl-coll-q cardinality entity
                               property args crd recursive)
   :recursive-q
   (get-get-query-function-for :recursive-q cardinality entity
                               property args crd recursive)})

(defmulti get-assoc-query-function-for (fn [for _ _ _ _ _ _] for))

(defmethod get-assoc-query-function-for :one-side-qs
  [for cardinality entity property args crd recursive]
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
                 :update-where (str "IN :tuple:" prop)})]))

(defmethod get-assoc-query-function-for :many-side-qs
  [for cardinality entity property args crd recursive]
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
                 :update-where (str "IN :tuple:" prop)})]))

(defn- make-insert-values
  [crd ent prop]
  (case crd
    :one-to-one (str "(:" ent ", :" prop ")")
    (make-hugsql-values-string (str ":" ent)
                               (str ":" prop))))

(defmethod get-assoc-query-function-for :oto&mtm-qs
  [for cardinality entity property args crd recursive]
  #(identity [(let [ent  (drill-out-name-for-clojure entity)
                    prop (drill-out-name-for-clojure (:name property))]
                {:assoc         true
                 :ent           ent :prop prop
                 :f-tbl         (build-additional-tbl-name
                                 cardinality entity property args)
                 :f-id          (make-id-column-name entity)
                 :s-id          (make-id-column-name (crd cardinality))
                 :insert-values (make-insert-values % ent prop)
                 :and-single    (if (= % :one-to-one) true false)})
              (let [ent  (drill-out-name-for-clojure (crd cardinality))
                    prop (drill-out-name-for-clojure (:back-property
                                                      cardinality))]
                {:assoc         true
                 :ent           ent :prop prop
                 :f-tbl         (build-additional-tbl-name
                                 cardinality entity property args)
                 :f-id          (make-id-column-name (crd cardinality))
                 :s-id          (make-id-column-name entity)
                 :insert-values (make-insert-values % ent prop)
                 :and-single    (if (= % :one-to-one) true false)})]))

(defmethod get-assoc-query-function-for :simple-collection
  [for cardinality entity property args crd recursive]
  #(identity (let [ent  (drill-out-name-for-clojure entity)
                   prop (drill-out-name-for-clojure (:name property))]
               [{:assoc         true
                 :ent           ent
                 :prop          prop
                 :f-tbl         (build-additional-tbl-name
                                 cardinality entity property args)
                 :f-id          (make-id-column-name entity)
                 :s-id          (drill-out-name-for-db (:name property))
                 :insert-values (make-insert-values crd ent prop)}])))

(defn- assoc-queries [cardinality entity property args crd recursive]
  {:one-side-qs
   (get-assoc-query-function-for :one-side-qs cardinality entity
                                 property args crd recursive)
   :many-side-qs
   (get-assoc-query-function-for :many-side-qs cardinality entity
                                 property args crd recursive)
   :oto&mtm-qs
   (get-assoc-query-function-for :oto&mtm-qs cardinality entity
                                 property args crd recursive)
   :simpl-coll-q
   (get-assoc-query-function-for :simple-collection cardinality entity
                                 property args crd recursive)
   :recursive-q ""})

(defmulti make-dissoc-all-q (fn [crd _ _] crd))

(defmethod make-dissoc-all-q :default
  [crd from update-id]
  (cond
    (not (#{:oto&mtm :one-to-one} crd))
    [(-> from
         (dissoc :assoc :f-id-val :s-id :update-where)
         (assoc :dissoc-all true
                :s-id update-id
                :update-where (str "= :" update-id)))]
    :default []))

(defmethod make-dissoc-all-q :simple-collection
  [crd from _]
  [(-> from (dissoc :update :assoc) (assoc :dissoc-all true
                                           :reverse-doc true))])

(defmulti make-dissoc-qs (fn [crd _] crd))

(defmethod make-dissoc-qs :default
  [crd queries]
  (-> #(assoc (dissoc % :assoc :f-id-val) :dissoc true)
      (mapv queries)))

(defmethod make-dissoc-qs :simple-collection
  [crd queries]
  (-> #(assoc (dissoc % :assoc :f-id-val) :dissoc true :reverse-doc true)
      (mapv queries)))

(defn assoc->dissoc-queries
  "Transforms assoc queries into dissoc queries key maps and eventually adds
  dissoc-all key map."
  [crd side params]
  (fn [] (let [side-key (case side
                          :one-side          :one-side-qs
                          :many-side         :many-side-qs
                          :oto&mtm           :oto&mtm-qs
                          :simple-collection :simpl-coll-q)
               qs       (case side-key
                          (or :one-side-qs :many-side-qs :simpl-coll-q)
                          ((side-key (apply assoc-queries params)))
                          :oto&mtm-qs
                          ((side-key (apply assoc-queries params)) crd))]
           (concat (make-dissoc-qs crd qs)
                   ;; dissoc-all queries
                   (if (= crd :simple-collection)
                     (make-dissoc-all-q crd (first qs) nil)
                     (make-dissoc-all-q crd (second qs) (:prop (first qs))))
                   (if (= crd :many-to-many)
                     (make-dissoc-all-q crd (first qs) (:prop (second qs))))))))

(defn- dissoc-queries
  [& [cardinality entity property args crd recursive :as p]]
  {:one-side-qs  (assoc->dissoc-queries :one-to-many :one-side p)
   :many-side-qs (assoc->dissoc-queries :one-to-many :many-side p)
   :oto&mtm-qs   #(assoc->dissoc-queries % :oto&mtm p)
   :simpl-coll-q (assoc->dissoc-queries :simple-collection :simple-collection p)
   :recursive-q  ""})

(defn queries
  [cardinality entity property args crd recursive]
  {:get    (get-queries cardinality entity property args crd recursive)
   :assoc  (assoc-queries cardinality entity property args crd recursive)
   :dissoc (dissoc-queries cardinality entity property args crd recursive)})

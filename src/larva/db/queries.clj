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

(defmethod get-get-query-function-for :recursive-qs
  [for cardinality entity property args crd recursive]
  #(identity (let [get-query
                   {:ent       (drill-out-name-for-clojure entity)
                    :prop      (drill-out-name-for-clojure (:name property))
                    :f-tbl     (build-db-table-name entity args)
                    :f-id      "id" :sign (if (= % :many-to-many) "IN" "=")
                    :s-id      (make-id-column-name entity)
                    :s-tbl     (build-additional-tbl-name
                                cardinality entity property args)
                    :t-id      (make-id-column-name entity true)
                    :sel-multi (true? (= % :many-to-many))}]
               [get-query
                ;; get reverse query
                (assoc get-query :get-rev true)])))

(defn- get-queries [cardinality entity property args crd recursive]
  (let [p [cardinality entity property args crd recursive]]
    {:one-side-qs
     ;; Generate query keys when it proceed a property witch represents "one" side
     ;; of an one-to-many relationship
     (apply get-get-query-function-for :one-side-qs p)
     :many-side-qs
     (apply get-get-query-function-for :many-side-qs p)
     :oto&mtm-qs
     (apply get-get-query-function-for :oto&mtm-qs p)
     :simpl-coll-qs
     (apply get-get-query-function-for :simpl-coll-q p)
     :recursive-qs
     (apply get-get-query-function-for :recursive-qs p)}))

(defn- make-insert-values
  [crd ent prop]
  (case crd
    :one-to-one (str "(:" ent ", :" prop ")")
    (make-hugsql-values-string (str ":" ent)
                               (str ":" prop))))

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

(defmethod get-assoc-query-function-for :recursive-qs
  [for cardinality entity property args crd recursive]
  #(identity (let [ent  (drill-out-name-for-clojure entity)
                   prop (drill-out-name-for-clojure (:name property))
                   assoc-query
                   {:assoc         true
                    :ent           ent :prop prop
                    :f-tbl         (build-additional-tbl-name
                                    cardinality entity property args)
                    :f-id          (make-id-column-name entity)
                    :s-id          (make-id-column-name (crd cardinality) true)
                    :insert-values (make-insert-values % ent prop)
                    :and-single    (if (= % :one-to-one) true false)}]
               [assoc-query
                ;; assoc reverse query
                (-> (dissoc assoc-query :assoc)
                    (assoc :assoc-rev true
                           :insert-values (make-insert-values % prop ent)))])))

(defn- assoc-queries [cardinality entity property args crd recursive]
  (let [p [cardinality entity property args crd recursive]]
    {:one-side-qs
     (apply get-assoc-query-function-for :one-side-qs p)
     :many-side-qs
     (apply get-assoc-query-function-for :many-side-qs p)
     :oto&mtm-qs
     (apply get-assoc-query-function-for :oto&mtm-qs p)
     :simpl-coll-qs
     (apply get-assoc-query-function-for :simple-collection p)
     :recursive-qs
     (apply get-assoc-query-function-for :recursive-qs p)}))

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

(defmethod make-dissoc-all-q :recursive
  [crd from _]
  [])

(defmulti make-dissoc-qs (fn [side _ _] side))

(defmethod make-dissoc-qs :default
  [side _ queries]
  (-> #(assoc (dissoc % :assoc :f-id-val) :dissoc true)
      (mapv queries)))

(defmethod make-dissoc-qs :simple-collection
  [side _ queries]
  (-> #(assoc (dissoc % :assoc :f-id-val) :dissoc true :reverse-doc true)
      (mapv queries)))

(defmethod make-dissoc-qs :recursive
  [side crd queries]
  (let [dissoc-query (-> (first queries)
                         (dissoc :assoc :f-id-val :assoc-rev)
                         (assoc :dissoc true))]
    [dissoc-query
     ;; dissoc reverse query
     (-> (dissoc dissoc-query :dissoc)
         (assoc :dissoc-rev true))]))

(defn assoc->dissoc-queries
  "Transforms assoc queries into dissoc queries key maps and eventually adds
  dissoc-all key map."
  [crd side params]
  (fn []
    ^{:break/when (and (= side :recursive) (= crd :one-to-one))}
    (let [side-key (case side
                     :one-side          :one-side-qs
                     :many-side         :many-side-qs
                     :oto&mtm           :oto&mtm-qs
                     :simple-collection :simpl-coll-qs
                     :recursive         :recursive-qs)
          ;; corresponding assoc queries
          qs       (case side-key
                     (:one-side-qs :many-side-qs :simpl-coll-qs)
                     ((side-key (apply assoc-queries params)))
                     (:oto&mtm-qs :recursive-qs)
                     ((side-key (apply assoc-queries params)) crd))]
      (concat (make-dissoc-qs side crd qs)
              ;; dissoc-all queries
              (if (= crd :simple-collection)
                (make-dissoc-all-q crd (first qs) nil)
                (make-dissoc-all-q crd (second qs) (:prop (first qs))))
              (if (= crd :many-to-many)
                (make-dissoc-all-q crd (first qs) (:prop (second qs))))))))

(defn- dissoc-queries
  [& [cardinality entity property args crd recursive :as p]]
  {:one-side-qs   (assoc->dissoc-queries :one-to-many :one-side p)
   :many-side-qs  (assoc->dissoc-queries :one-to-many :many-side p)
   :oto&mtm-qs    #(assoc->dissoc-queries % :oto&mtm p)
   :simpl-coll-qs (assoc->dissoc-queries :simple-collection :simple-collection p)
   :recursive-qs  #(assoc->dissoc-queries % :recursive p)})

(defn queries
  [cardinality entity property args crd recursive]
  {:get    (get-queries cardinality entity property args crd recursive)
   :assoc  (assoc-queries cardinality entity property args crd recursive)
   :dissoc (dissoc-queries cardinality entity property args crd recursive)})

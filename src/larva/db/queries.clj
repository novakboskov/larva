(ns larva.db.queries
  "This namespace contains functions which serve to create maps that are
  later used to fill additional queries template."
  (:require [clojure.set :as set]
            [larva.db
             [commons :refer :all]
             [utils :refer :all]]))

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
               :f-id      "id" :sign (if (= crd :many-to-many) "IN" "=")
               :s-id      (get-db-table-id (crd cardinality) args)
               :s-tbl     (build-additional-tbl-name
                           cardinality entity property args)
               :t-id      (get-db-table-id entity args)
               :sel-multi (true? (= crd :many-to-many))}
              {:ent       (drill-out-name-for-clojure (crd cardinality))
               :prop      (drill-out-name-for-clojure (:back-property
                                                       cardinality))
               :f-tbl     (build-db-table-name entity args)
               :f-id      "id" :sign (if (= crd :many-to-many) "IN" "=")
               :s-id      (get-db-table-id entity args)
               :s-tbl     (build-additional-tbl-name
                           cardinality entity property args)
               :t-id      (get-db-table-id (crd cardinality) args)
               :sel-multi (true? (= crd :many-to-many))}]))

(defmethod get-get-query-function-for :simpl-coll-q
  [for cardinality entity property args crd recursive]
  #(identity [{:ent     (drill-out-name-for-clojure entity)
               :prop    (drill-out-name-for-clojure (:name property))
               :f-tbl   (build-additional-tbl-name
                         cardinality entity property args)
               :f-id    (get-db-table-id entity args) :sign      "="
               :no-nest true                          :sel-multi true}]))

(defmethod get-get-query-function-for :recursive-qs
  [for cardinality entity property args crd recursive]
  #(identity (let [get-query
                   {:ent       (drill-out-name-for-clojure entity)
                    :prop      (drill-out-name-for-clojure (:name property))
                    :f-tbl     (build-db-table-name entity args)
                    :f-id      "id" :sign (if (= crd :many-to-many) "IN" "=")
                    :s-id      (get-db-table-id entity args)
                    :s-tbl     (build-additional-tbl-name
                                cardinality entity property args)
                    :t-id      (get-db-table-id entity args true)
                    :sel-multi (true? (= crd :many-to-many))}]
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
     :smpl-coll-qs
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
                 :f-id          (get-db-table-id entity args)
                 :s-id          (get-db-table-id (crd cardinality) args)
                 :insert-values (make-insert-values crd ent prop)
                 :and-single    (if (= crd :one-to-one) true false)})
              (let [ent  (drill-out-name-for-clojure (crd cardinality))
                    prop (drill-out-name-for-clojure (:back-property
                                                      cardinality))]
                {:assoc         true
                 :ent           ent :prop prop
                 :f-tbl         (build-additional-tbl-name
                                 cardinality entity property args)
                 :f-id          (get-db-table-id (crd cardinality) args)
                 :s-id          (get-db-table-id entity args)
                 :insert-values (make-insert-values crd ent prop)
                 :and-single    (if (= crd :one-to-one) true false)})]))

(defmethod get-assoc-query-function-for :simple-collection
  [for cardinality entity property args crd recursive]
  #(identity (let [ent  (drill-out-name-for-clojure entity)
                   prop (drill-out-name-for-clojure (:name property))]
               [{:assoc         true
                 :ent           ent
                 :prop          prop
                 :f-tbl         (build-additional-tbl-name
                                 cardinality entity property args)
                 :f-id          (get-db-table-id entity args)
                 :s-id          (drill-out-name-for-db (:name property))
                 :insert-values (make-insert-values crd ent prop)}])))

(defmethod get-assoc-query-function-for :recursive-qs
  [for cardinality entity property args crd recursive]
  (fn []
    (let [ent  (drill-out-name-for-clojure entity)
          prop (drill-out-name-for-clojure (:name property))
          assoc-query
          {:assoc         true
           :ent           ent :prop prop
           :f-tbl         (build-additional-tbl-name
                           cardinality entity property args)
           :f-id          (get-db-table-id entity args)
           :s-id          (get-db-table-id (crd cardinality) args true)
           :insert-values (make-insert-values crd ent prop)
           :and-single    (if (= crd :one-to-one) true false)}]
      [assoc-query
       ;; assoc reverse query
       (-> (dissoc assoc-query :assoc)
           (assoc :assoc-rev true
                  :insert-values (make-insert-values crd prop ent))
           (#(case crd :many-to-many
                   (set/rename-keys % {:f-id :s-id
                                       :s-id :f-id}) %)))])))

(defn- assoc-queries [cardinality entity property args crd recursive]
  (let [p [cardinality entity property args crd recursive]]
    {:one-side-qs
     (apply get-assoc-query-function-for :one-side-qs p)
     :many-side-qs
     (apply get-assoc-query-function-for :many-side-qs p)
     :oto&mtm-qs
     (apply get-assoc-query-function-for :oto&mtm-qs p)
     :smpl-coll-qs
     (apply get-assoc-query-function-for :simple-collection p)
     :recursive-qs
     (apply get-assoc-query-function-for :recursive-qs p)}))

(defn make-dissoc-all-q-dispatch [side crd _ _]
  (case [side crd]
    [:recursive :many-to-many] :many-to-many-r
    crd))

(defmulti make-dissoc-all-q
  "Build dissoc all queries from assoc ones"
  #'make-dissoc-all-q-dispatch)

(defmethod make-dissoc-all-q :default
  [_ crd first second]
  (let [update-id (:prop first)]
    (case crd
      (:one-to-many :many-to-one)
      [(-> second
           (dissoc :assoc :f-id-val :s-id :update-where)
           (assoc :dissoc-all true
                  :s-id update-id
                  :update-where (str "= :" update-id)))]
      [])))

(defmethod make-dissoc-all-q :simple-collection
  [_ _ first  _]
  [(-> first (dissoc :update :assoc) (assoc :dissoc-all true
                                            :reverse-doc true))])

(defmethod make-dissoc-all-q :many-to-many
  [_ _ first second]
  (let [base-fn #(-> (dissoc %1 :assoc :f-id-val :s-id :update-where)
                     (assoc :dissoc-all true
                            :s-id %2
                            :update-where (str "= :" %2)))]
    [(base-fn second (:prop first))
     (base-fn first (:prop second))]))

(defmethod make-dissoc-all-q :many-to-many-r
  [_ _ first second]
  (let [base-fn (fn [f s reverse-name]
                  (-> (dissoc f :assoc :f-id-val :s-id :update-where :assoc-rev)
                      (assoc :dissoc-all true
                             :s-id s
                             :update-where (str "= :" s))
                      (#(if reverse-name (assoc % :name-rev true) %))))]
    [(base-fn first (:prop second) false)
     (base-fn second (:prop first) true)]))

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
    (let [side-key (case side
                     :one-side          :one-side-qs
                     :many-side         :many-side-qs
                     :oto&mtm           :oto&mtm-qs
                     :simple-collection :smpl-coll-qs
                     :recursive         :recursive-qs)
          ;; corresponding assoc queries
          qs       (case side-key
                     (:one-side-qs :many-side-qs :smpl-coll-qs)
                     ((side-key (apply assoc-queries params)))
                     (:oto&mtm-qs :recursive-qs)
                     ((side-key (apply assoc-queries params))))]
      (concat (make-dissoc-qs side crd qs)
              ;; dissoc-all queries
              (make-dissoc-all-q side crd (first qs) (second qs))))))

(defn- dissoc-queries
  [& [cardinality entity property args crd recursive :as p]]
  {:one-side-qs  (assoc->dissoc-queries crd :one-side p)
   :many-side-qs (assoc->dissoc-queries crd :many-side p)
   :oto&mtm-qs   (assoc->dissoc-queries crd :oto&mtm p)
   :smpl-coll-qs (assoc->dissoc-queries crd :simple-collection p)
   :recursive-qs (assoc->dissoc-queries crd :recursive p)})

(defn queries
  [cardinality entity property args crd recursive]
  {:get    (get-queries cardinality entity property args crd recursive)
   :assoc  (assoc-queries cardinality entity property args crd recursive)
   :dissoc (dissoc-queries cardinality entity property args crd recursive)})

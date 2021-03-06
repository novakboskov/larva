(ns larva.graph
  "Provides functions for transforming standard application program (uni-model) to its graph representation.
  Graph representation is further used in program interpretation and code generation."
  (:require [clojure.edn :as edn]
            [larva.meta-model :refer :all]
            [schema.core :as s]
            [ubergraph.core :as g]))

(defn node-uuid []
  (java.util.UUID/randomUUID))

(def about-node-label :about)

(def meta-node-label :meta)

(def root-node :program)

(def entities-node :entities)

(def reference-type :reference)

(defn build-entity-node-label [entity]
  (:signature entity))

(defn build-entity-edge-label [order]
  (str "entities: " order))

(s/defn ^:always-validate build-property-label :- s/Str
  [prop :- Property parent :- s/Str]
  (str (:name prop) "#" parent))

(s/defn ^:always-validate build-property-label-from-str :- s/Str
  [prop-name :- s/Str parent :- s/Str]
  (str prop-name "#" parent))

(defn build-property-edge-label [next-order]
  (str "property: " next-order))

(defn build-about-edge-label [cmd-order]
  (str "about: " cmd-order))

(defn build-meta-node-label [cmd-order]
  (str "about: " cmd-order))

(s/defn ^:always-validate build-reference-edge-label :- s/Str
  [prop-type :- SomethingWithReference
   order :- s/Int]
  (cond
    (contains? prop-type :coll) (str "reference:[" order "] collection")
    (contains? prop-type :one)  (str "reference:[" order "] one")))

(s/defn ^{:always-validate true :private true} build-property-map
  [property :- Property]
  (let [type      (if-let [pt (-> property :type)] {:type pt} {})
        gui-label (if-let [gl (-> property :gui-label)] {:gui-label gl} {})
        name      {:name (:name property)}
        uuid      {:uuid (node-uuid)}]
    (merge name uuid type gui-label)))

(s/defn ^{:always-validate true :private true}
  build-entity-map
  [entity :- Entity]
  (let [signature {:signature (:signature entity)}
        uuid      {:uuid (node-uuid)}
        plural    (if-let [pl (:plural entity)] {:plural pl} {})]
    (merge signature uuid plural)))

(s/defn ^{:always-validate true :private true} build-about-map
  [about :- About]
  (let [name    (if-let [name (:name about)] {:name name} {})
        author  (if-let [author (:author about)] {:author author} {})
        comment (if-let [comment (:comment about)] {:comment comment} {})
        uuid    {:uuid (node-uuid)}]
    (merge name author comment uuid)))

(s/defn ^{:always-validate true :private true} build-meta-data-map
  [meta-data :- Meta]
  (let [api-only (if-let [ao (-> (:api-only meta-data) nil? not)] {:api-only ao} {})
        db       (if-let [db (:db meta-data)] {:db db} {})
        uuid     {:uuid (node-uuid)}]
    (merge api-only db uuid)))

(s/defn ^{:always-validate true :private true}
  add-property-reference :- {:graph      ubergraph.core.Ubergraph
                             :next-order s/Int}
  "Add property->entity references."
  [graph :- ubergraph.core.Ubergraph property :- Property
   parent :- s/Str next-order :- s/Int]
  (if (or (= :reference (get-in property [:type :coll]))
          (= :reference (get-in property [:type :one])))
    {:graph      (g/add-edges graph [(build-property-label property parent)
                                     (get-in property [:type :to 0])
                                     {:type  reference-type
                                      :back-property
                                      (get-in property [:type :to 1])
                                      :label (build-reference-edge-label
                                              (:type property) next-order)
                                      :cardinality
                                      (cond
                                        (contains? (:type property) :coll) :coll
                                        (contains? (:type property) :one)  :one)
                                      :order next-order :color :green}])
     :next-order (inc next-order)}
    {:graph graph :next-order next-order}))

(s/defn ^{:always-validate true :private true}
  add-properties :- {:graph ubergraph.core.Ubergraph :next-order s/Int}
  "Adds properties of an entity."
  [graph :- ubergraph.core.Ubergraph parent :- s/Str
   properties :- Properties
   next-order :- s/Int]
  (loop [g graph props properties po next-order]
    (if (not-empty props)
      (let [prop-label (build-property-label (first props) parent)
            g-w-property
            (-> (g/add-nodes-with-attrs g [prop-label
                                           (build-property-map (first props))])
                (g/add-edges [parent prop-label {:label
                                                 (build-property-edge-label po)
                                                 :order po}])
                (add-property-reference (first props) parent (inc po)))]
        (recur (:graph g-w-property) (rest props) (:next-order g-w-property)))
      {:graph g :next-order po})))

(s/defn ^{:always-validate true :private true}
  add-entitiy-node :- {:graph      ubergraph.core.Ubergraph :next-order s/Int
                       :node-label s/Str}
  "Adds entity node."
  [graph :- ubergraph.core.Ubergraph
   entity :- Entity entity-order :- s/Int]
  (let [entity-label (build-entity-node-label entity)]
    {:graph
     (-> (g/add-nodes-with-attrs graph [entity-label
                                        (build-entity-map entity)])
         (g/add-edges [entities-node entity-label
                       {:label (build-entity-edge-label entity-order)
                        :order entity-order}]))
     :next-order (inc entity-order)
     :node-label entity-label}))

(s/defn ^{:always-validate true :private true}
  add-whole-entity :- {:graph ubergraph.core.Ubergraph :next-order s/Int}
  "Extend graph with entity and all its successors. Returns graph and
entity-order for next entity."
  [graph :- ubergraph.core.Ubergraph
   entity :- Entity entity-order]
  (let [g-w-node  (add-entitiy-node graph entity entity-order)
        g-w-props (add-properties (:graph g-w-node) (:node-label g-w-node)
                                  (:properties entity) (:next-order g-w-node))]
    {:graph      (:graph g-w-props)
     :next-order (:next-order g-w-props)}))

(s/defn ^{:always-validate true :private true}
  add-entities-beginning-node :- {:graph      ubergraph.core.Ubergraph
                                  :next-order s/Int}
  [graph :- ubergraph.core.Ubergraph order :- s/Int]
  {:graph
   (-> (g/add-nodes graph entities-node)
       (g/add-edges [root-node entities-node {:label (str "begin-entities: "
                                                          order)
                                              :order order}]))
   :next-order (inc order)})

(s/defn ^{:always-validate true :private true}
  add-about :- {:graph ubergraph.core.Ubergraph :next-order s/Int}
  "Adds whole about node."
  [graph :- ubergraph.core.Ubergraph
   about :- About cmd-order :- s/Int]
  {:graph
   (-> (g/add-nodes-with-attrs graph [about-node-label
                                      (build-about-map about)])
       (g/add-edges [root-node about-node-label {:label (build-about-edge-label cmd-order)
                                                 :order cmd-order}]))
   :next-order (inc cmd-order)})

(s/defn ^{:always-validate true :private true}
  add-meta :- {:graph ubergraph.core.Ubergraph :next-order s/Int}
  "Adds whole meta node."
  [graph :- ubergraph.core.Ubergraph
   meta-data :- Meta cmd-order :- s/Int]
  {:graph
   (-> (g/add-nodes-with-attrs graph [meta-node-label
                                      (build-meta-data-map meta-data)])
       (g/add-edges [root-node meta-node-label {:label (build-meta-node-label cmd-order)
                                                :order cmd-order}]))
   :next-order (inc cmd-order)})

(s/defn ^:always-validate ->graph :- ubergraph.core.Ubergraph
  "Transforms standard application program (uni-model) to its graph representation."
  [program :- Program]
  (let [about            (:about program)
        meta_data        (:meta program)
        bare-graph       {:graph (g/digraph root-node) :next-order 1}
        graph-with-about (if about (add-about (:graph bare-graph) about 1)
                             bare-graph)
        graph-with-meta  (if meta_data
                           (add-meta (:graph graph-with-about) meta_data
                                     (:next-order graph-with-about))
                           graph-with-about)
        entities         (:entities program)
        graph            (if (> (count entities) 0)
                           (add-entities-beginning-node (:graph graph-with-meta)
                                                        (:next-order graph-with-meta))
                           graph-with-meta)
        entity-order     (:next-order graph)]
    ;; adding entities to graph
    (loop [g graph ents entities eo entity-order]
      (if (not-empty ents)
        (let [entity (add-whole-entity (:graph g) (first ents) eo)]
          (recur entity (rest ents) (:next-order entity)))
        (:graph g)))))

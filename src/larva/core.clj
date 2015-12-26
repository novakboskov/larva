(ns larva.core
  (:require [larva.meta-model :refer :all]
            [schema.core :as s]
            [ubergraph.core :as g]))

(def uni-model (clojure.edn/read-string (slurp "resources/edn-sources/standard_app.clj")))

;;;;;;
;; Transforming standard app program to Ubergraph
;;;;;;

(defn node-uuid []
  (java.util.UUID/randomUUID))

(def about-node-label "about")

(def root-node :program)

(def entities-node :entities)

(defn build-entity-node-label [entity]
  (:signature entity))

(defn build-entity-edge-label [order]
  (str "entities: " order))

(defn build-property-label [prop parent]
  (str (:name prop) "#" parent))

(defn build-property-edge-label [next-order]
  (str "property: " next-order))

(defn build-about-edge-label [cmd-order]
  (str "about: " cmd-order))

(s/defn ^:always-validate add-properties :- {:graph ubergraph.core.Ubergraph
                                             :next-order s/Int}
  "Adds properties of an entity."
  [graph :- ubergraph.core.Ubergraph parent :- s/Str
   properties :- Properties
   next-order :- s/Int]
  (loop [g graph props properties po next-order]
    (if (> (count props) 0)
      (let [prop-label (build-property-label (first props) parent)
            g-w-property
            (-> (g/add-nodes-with-attrs g [prop-label
                                           {:name (:name (first props))
                                            :type (:type (first props))
                                            :gui-label
                                            (:gui-label (first props))
                                            :uuid (node-uuid)}])
                (g/add-edges [parent prop-label {:label
                                                 (build-property-edge-label
                                                  po)}]))]
        (recur g-w-property (rest props) (inc po)))
      {:graph g :next-order po})))

(s/defn ^:always-validate add-entitiy-node :- {:graph ubergraph.core.Ubergraph
                                               :next-order s/Int
                                               :node-label s/Str}
  "Adds entity node."
  [graph :- ubergraph.core.Ubergraph
   entity :- Entity entity-order :- s/Int]
  (let [entity-label (build-entity-node-label entity)]
    {:graph
     (-> (g/add-nodes graph entity-label)
         (g/add-edges [entities-node entity-label
                       {:label (build-entity-edge-label entity-order)
                        :order entity-order
                        :uuid (node-uuid)}]))
     :next-order (inc entity-order)
     :node-label entity-label}))

(s/defn ^:always-validate add-whole-entity :- {:graph ubergraph.core.Ubergraph
                                               :next-order s/Int}
  "Extend graph with entity and all its successors. Returns graph and
entity-order for next entity."
  [graph :- ubergraph.core.Ubergraph
   entity :- Entity entity-order]
  (let [g-w-node (add-entitiy-node graph entity entity-order)
        g-w-props (add-properties (:graph g-w-node) (:node-label g-w-node)
                                  (:properties entity) (:next-order g-w-node))]
    {:graph (:graph g-w-props)
     :next-order (:next-order g-w-props)}))

(s/defn ^:always-validate add-entities-beginning-node :-
  {:graph ubergraph.core.Ubergraph
   :next-order s/Int}
  [graph :- ubergraph.core.Ubergraph order :- s/Int]
  {:graph
   (-> (g/add-nodes graph entities-node)
       (g/add-edges [root-node entities-node {:label (str "begin-entities: "
                                                          order)
                                              :order order}]))
   :next-order (inc order)})

(s/defn ^:always-validate add-about :- {:graph ubergraph.core.Ubergraph
                                        :next-order s/Int}
  "Adds whole about node."
  [graph :- ubergraph.core.Ubergraph
   about cmd-order]
  {:graph
   (-> (g/add-nodes-with-attrs graph [about-node-label
                                      {:name (:name about)
                                       :author (:author about)
                                       :comment (:comment about)
                                       :uuid (node-uuid)}])
       (g/add-edges [root-node about-node-label {:label (build-about-edge-label cmd-order)
                                                 :order cmd-order}]))
   :next-order (inc cmd-order)})

(s/defn ^:always-validate to-graph :- ubergraph.core.Ubergraph
  "Transforms standard app program EDN to Ubergraph."
  [program :- Program]
  (let [about (:about program)
        bare-graph {:graph (g/digraph root-node) :next-order 1}
        graph-with-about (if about (add-about (:graph bare-graph) about 1)
                             bare-graph)
        entities (:entities program)
        graph (if (> (count entities) 0)
                (add-entities-beginning-node (:graph graph-with-about)
                                             (:next-order graph-with-about))
                graph-with-about)
        entity-order (:next-order graph)]
    ;; adding entities to graph
    (loop [g graph ents entities eo entity-order]
      (if (> (count ents) 0)
        (let [entity (add-whole-entity (:graph g) (first ents) eo)]
          (recur entity (rest ents) (:next-order entity)))
        (:graph g)))))

;;;;;; play
;; (g/viz-graph (to-graph uni-model))
;;;;;;

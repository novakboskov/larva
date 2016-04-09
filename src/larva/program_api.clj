(ns larva.program-api
  "API to inner program representation."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [larva
             [graph :as g]
             [program-api-schemes :refer :all]
             [utils :refer [parse-project-clj]]]
            [schema.core :as s]
            [ubergraph.core :as u]))

(defonce ^:private program-model (atom nil))
(def default-larva-dir "larva_src")
(def default-model-path
  (.getPath (io/file default-larva-dir "larva.clj")))

(defmulti ^:private sort-by-edge
  (fn [first second & third] (if third :properties :entities)))

(defmethod sort-by-edge :entities
  [entities program]
  (sort-by #(:order (u/attrs program (u/find-edge program g/entities-node %)))
           entities))

(defmethod sort-by-edge :properties
  [properties entity program]
  (sort-by #(:order (u/attrs program (u/find-edge program entity %))) properties))

(defn project-name
  "Return project name. Parsing project.clj if it is available."
  []
  (str (nth (parse-project-clj) 1)))

(defn reset-program-model
  "Sets program model to be used."
  [model]
  (reset! program-model model))

(defn destroy-program-model
  "Destroys program model."
  []
  (reset! program-model nil))

(defn model->program
  "Resolve program model in following order:
  - program-model atom
  - provided :model key
  - provided :model-path key
  - default-model-path"
  [& {:keys [path model] :or {path default-model-path}}]
  (if-let [model-atom @program-model] (g/->graph model-atom)
          (if model (g/->graph model) (-> path slurp edn/read-string g/->graph))))

(defn- resolve-program [{:keys [model-path model]}]
  (cond model      (model->program :model model)
        model-path (model->program :path model-path)
        :else      (model->program)))

(defn- get-all-entities
  [program]
  (vec (sort-by-edge (u/successors program g/entities-node) program)))

(s/defn ^:always-validate all-entities :- APIEntities
  "Returns signatures of all entities in program.
   Path to program model can be supplied otherwise default is be used.
   Model itself also can be supplied."
  ([] (get-all-entities (model->program)))
  ([{:keys [model-path model] :as model-options}]
   (get-all-entities (resolve-program model-options))))

(defn- get-entity-info
  [p entity]
  (dissoc (u/attrs p entity) :uuid))

(s/defn ^:always-validate entity-info :- APIEntityInfo
  "Returns information about entity itself."
  ([entity :- s/Str]
   (get-entity-info (model->program) entity))
  ([entity :- s/Str {:keys [model model-path] :as model-options}]
   (get-entity-info (resolve-program model-options) entity)))

(defn- get-entity-properties
  [program entity]
  (mapv #(dissoc (u/attrs program %) :uuid)
        (sort-by-edge (u/successors program entity) entity program)))

(s/defn ^:always-validate entity-properties :- APIProperties
  "Returns properties of an entity given by signature.
   Path to program model can be supplied otherwise default is be used.
   Model itself also can be supplied."
  ([entity :- s/Str]
   (get-entity-properties (model->program) entity))
  ([entity :- s/Str {:keys [model-path model] :as model-options}]
   (get-entity-properties (resolve-program model-options) entity)))

(defn- get-back-ref&back-prop
  [program entity property reference ref-dest explicit-back-property]
  (and ref-dest
       (cond
         explicit-back-property
         (let [prop-label
               (g/build-property-label-from-str explicit-back-property ref-dest)]
           [(filter #(= entity (u/dest %)) (u/out-edges program prop-label))
            explicit-back-property])
         (not explicit-back-property)
         (nth (->> (u/successors program ref-dest)
                   (map
                    (fn [successor]
                      [(filter
                        #(and (= g/reference-type (u/attr program % :type))
                              (= entity (u/dest %))
                              (= (g/build-property-label property entity)
                                 (g/build-property-label-from-str
                                  (u/attr program % :back-property) entity)))
                        (u/out-edges program successor))
                       (u/attr program successor :name)]))
                   (remove #(empty? (first %)))) 0))))

(defn- get-property-reference [program entity property]
  (let [reference (first
                   (->> (g/build-property-label property entity)
                        (u/out-edges program)
                        (filter #(= (u/attr program % :type) g/reference-type))))
        ref-dest  (if reference (u/dest reference))
        explicit-back-property
        (if ref-dest (u/attr program reference :back-property))
        [[back-ref] back-prop]
        (if ref-dest (get-back-ref&back-prop
                      program entity property reference ref-dest
                      explicit-back-property))
        src-crd   (if reference (u/attr program reference :cardinality))
        dest-crd  (if back-ref (u/attr program back-ref :cardinality))
        back-prop (if back-prop {:back-property back-prop})
        recursive (if (= reference back-ref) {:recursive true})]
    (cond (not reference)  {}
          (and back-ref (and (= src-crd :coll) (= dest-crd :coll)))
          (merge {:many-to-many ref-dest} back-prop recursive)
          (and back-ref (and (= src-crd :one) (= dest-crd :one)))
          (merge {:one-to-one ref-dest} back-prop recursive)
          (and back-ref (and (= src-crd :coll) (= dest-crd :one)))
          (merge {:many-to-one ref-dest} back-prop recursive)
          (= src-crd :one) (merge {:one-to-many ref-dest} back-prop recursive))))

(s/defn ^:always-validate property-reference :- APIPropertyReference
  "Returning the map consisted of reference type and signature of entity that is
   referenced by certain property alongside with and recursion indicator."
  ([entity :- s/Str property :- APIProperty]
   (get-property-reference (model->program) entity property))
  ([entity :- s/Str property :- APIProperty {:keys [model-path model] :as model-options}]
   (get-property-reference (resolve-program model-options) entity property)))

(defn- get-program-meta
  [program]
  (if (contains? (u/nodes program) g/meta-node-label)
    (dissoc (u/attrs program g/meta-node-label) :uuid) {}))

(s/defn program-meta :- APIMeta
  "Returns meta section of a program."
  ([] (get-program-meta (model->program)))
  ([{:keys [model-path model] :as model-options}]
   (get-program-meta (resolve-program model-options))))

(defn- get-program-about
  [program]
  (if (contains? (u/nodes program) g/about-node-label)
    (dissoc (u/attrs program g/about-node-label) :uuid) {}))

(s/defn program-about :- APIAbout
  "Returns meta section of program."
  ([] (get-program-about (model->program)))
  ([{:keys [model-path model] :as model-options}]
   (get-program-about (resolve-program model-options))))

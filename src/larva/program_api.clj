(ns larva.program-api
  "API to inner program representation."
  (:require [clojure.edn :as edn]
            [larva
             [program-api-schemes :refer :all]
             [graph :as g]]
            [schema.core :as s]
            [ubergraph.core :as u]))

(defonce ^:private program-model (atom nil))

(defn- extract-property-name [property]
  (first (clojure.string/split property #"#")))

(def ^:private default-model-path "larva_src/larva.clj")

(defn reset-program-model
  "Sets program model to be used."
  [model]
  (reset! program-model model))

(defn destroy-program-model
  "Destroys program model."
  []
  (reset! program-model nil))

(defn model->program
  "Make graph representation of a program specified through meta-model.
  If program-model atom is present it is used regardless, otherwise:
  If path to file which contains meta-model is not specified then <project-root>/larva-src/larva.clj
  is used instead. If model key is supplied then it is used to produce program."
  [& {:keys [path model] :or {path default-model-path}}]
  (if-let [model-atom @program-model] (g/->graph model-atom)
          (if model (g/->graph model) (-> path slurp edn/read-string g/->graph))))

(defn- resolve-model-source [{:keys [model-path model]}]
  (cond model (model->program :model model)
        model-path (model->program :path model-path)
        :else (model->program)))

(s/defn ^:always-validate all-entities :- APIEntities
  "Returns signatures of all entities in program.
   Path to program model can be supplied otherwise default is be used.
   Model itself also can be supplied."
  ([] (vec (u/successors (model->program) g/entities-node)))
  ([{:keys [model-path model] :as model-options}]
   (let [p (resolve-model-source model-options)]
     (vec (u/successors p g/entities-node)))))

(s/defn ^:always-validate entity-info :- APIEntityInfo
  "Returns information about entity itself."
  ([entity :- s/Str]
   (let [p (model->program)]
     (dissoc (u/attrs p entity) :uuid)))
  ([entity {:keys [model model-path] :as model-options}]
   (let [p (resolve-model-source model-options)]
     (dissoc (u/attrs p entity) :uuid))))

(s/defn ^:always-validate entity-properties :- APIProperties
  "Returns properties of an entity given by signature.
   Path to program model can be supplied otherwise default is be used.
   Model itself also can be supplied."
  ([entity :- s/Str]
   (let [p (model->program)]
     (mapv #(dissoc (u/attrs p %) :uuid) (u/successors p entity))))
  ([entity {:keys [model-path model] :as model-options}]
   (let [p (resolve-model-source model-options)]
     (mapv #(dissoc (u/attrs p %) :uuid) (u/successors p entity)))))

;;;;;; play
;; (u/viz-graph (model->program))
;; (entity-properties (first (all-entities)))
;;;;;;

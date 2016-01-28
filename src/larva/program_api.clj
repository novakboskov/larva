(ns larva.program-api
  "API to inner program representation."
  (:require [clojure.edn :as edn]
            [larva
             [program-api-schema :refer :all]
             [graph :as g]]
            [schema.core :as s]
            [ubergraph.core :as u]))

(defn- extract-property-name [property]
  (first (clojure.string/split property #"#")))

(s/defn ^:always-validate model->program :- ubergraph.core.Ubergraph
  "Make graph representation of a program specified through meta-model.
  If path to file which contains meta-model is not specified then <project-root>/larva-src/larva.clj
  is used instead."
  ([{:keys [path]}] (-> path slurp edn/read-string g/->graph))
  ([] (-> "larva-src/larva.clj" slurp edn/read-string g/->graph)))

(s/defn ^:always-validate all-entities :- APIEntities
  "Returns signatures of all entities in program."
  [program :- ubergraph.core.Ubergraph]
  (u/successors program g/entities-node))

(s/defn ^:always-validate entity-properties :- APIProperties
  "Returns properties of an entity given by signature."
  [graph :- ubergraph.core.Ubergraph entity :- s/Str]
  (mapv #(dissoc (u/attrs graph %) :uuid) (u/successors graph entity)))

;;;;;; play
;; (u/viz-graph (model->program {:path "resources/edn-sources/standard_app.edn"}))

;; (let [g (model->program {:path "resources/edn-sources/standard_app.edn"})]
;;   (->> (all-entities g)
;;        first
;;        (entity-properties g)))

;;;;;;

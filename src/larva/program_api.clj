(ns larva.program-api
  "API to inner program representation."
  (:require [clojure.edn :as edn]
            [larva
             [graph :as g]
             [program-api-schemes :refer :all]]
            [schema.core :as s]
            [ubergraph.core :as u]
            [larva.utils :refer [parse-project-clj]]))

(defonce ^:private program-model (atom nil))

(defn- extract-property-name [property]
  (first (clojure.string/split property #"#")))

(defmulti ^:private sort-by-edge
  (fn [first second & third] (if third :properties :entities)))

(defmethod sort-by-edge :entities
  [entities program]
  (sort-by #(:order (u/attrs program (u/find-edge program g/entities-node %)))
           entities))

(defmethod sort-by-edge :properties
  [properties entity program]
  (sort-by #(:order (u/attrs program (u/find-edge program entity %))) properties))

(def ^:private default-model-path "larva_src/larva.clj")

(defn project-name
  "Return project name. Parsing project.clj if it is available."
  []
  (try
    (parse-project-clj (slurp "project.clj"))
    (catch Exception e (str "project.clj is not fund in root of your project: "
                            (.getMessage e)))))

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

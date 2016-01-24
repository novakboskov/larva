(ns larva.model-utils
  (:require [clojure.edn :as edn]
            [larva.graph :refer [->graph]]
            [schema.core :as s]
            [ubergraph.core :as g]))

(s/defn model->program :- ubergraph.core.Ubergraph
  "Make graph representation of a program specified through meta-model.
  If path to file which contains meta-model is not specified then <project-root>/larva-src/larva.clj
  is used instead."
  ([{:keys [path]}] (-> path slurp edn/read-string ->graph))
  ([] (-> "larva-src/larva.clj" slurp edn/read-string ->graph)))

;;;;;; play
;; (g/viz-graph (model->program {:path "resources/edn-sources/standard_app.edn"}))
;;;;;;

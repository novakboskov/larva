(ns larva.frameworks.luminus.build
  (:require [clojure.java.io :as io]
            [larva.db.utils :as db]
            [larva.program-api :as api]
            [leiningen.new.templates :refer [->files]]))

(defn add-database-layer
  [{:keys [model-path model]}]
  (let [args (cond model-path {:model-path model-path}
                   model {:model model}
                   :else nil)
        entities (if args (api/all-entities args) (api/all-entities))
        ent-props (map #(if args {% (api/entity-properties % args)}
                            {% (api/entity-properties %)}) entities)]
    (doseq [ent ent-props]
      (let [ent-sign (first (keys ent))
            props (first (vals ent))
            entity-plural (db/build-plural-for-entity ent-sign args)
            properties (db/build-sequence-string props :insert)
            values-properties (db/build-sequence-string props :values)
            set-properties (db/build-sequence-string props :set)]
        ;; TODO: generate files, track namespaces which are changed, those need to be reloaded later.
        ))))

(defn make
  "Generate Luminus project from larva meta-model pointed to by path.
  options are represented by a set which can contain following keys:
  * model-only - generates only database(model) related code."
  [& {:keys [model-path model options] :as args}]
  (-> args
      add-database-layer))

;;;;;;play
(make)
;; (->files {:name "this-and-here"} ["ovde/je/ovo.clj" "Hello World!"])
;; (defn resource [r]
;;   (->> r (str "larva/frameworks/luminus/assets/core/resources/") (io/resource)))
;;;;;;

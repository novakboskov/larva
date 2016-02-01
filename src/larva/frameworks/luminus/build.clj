(ns larva.frameworks.luminus.build
  (:require [larva.db.utils :as db]
            [larva.program-api :as api]
            [leiningen.new.templates :refer [project-name sanitize-ns
                                             name-to-path]]))

(defn- build-api-args-map [{:keys [model-path model]}]
  (cond model-path {:model-path model-path}
        model {:model model}
        :else nil))

(defn add-database-layer
  [options]
  (let [args (build-api-args-map options)
        entities (if args (api/all-entities args) (api/all-entities))]
    (doseq [ent-sign entities]
      (let [props (if args (api/entity-properties ent-sign args)
                      (api/entity-properties ent-sign))
            entity-plural (db/build-plural-for-entity ent-sign args)
            properties (db/build-sequence-string props :insert)
            values-properties (db/build-sequence-string props :values)
            set-properties (db/build-sequence-string props :set)]
        ;; TODO: generate files, track namespaces which are changed, those need to be reloaded later.
        ;; Better solutions is to make a macro which evaluates code that produce SQL queries from .sql files in corresponding namespace
        ;; then to produce complete new file resources/templates/frameworks/luminus/larva-specific/db/src/sql.db.clj
        ;; maybe user wants to add some more code in this file, model refreshing should not affect that code.
        (render-assets [] (:render-options options))))))

(defn make
  "Generate Luminus project from larva meta-model pointed to by path.
  options are represented by a set which can contain following keys:
  * model-only - generates only database(model) related code."
  [& {:keys [model-path model options] :as args}]
  (let [name (api/project-name)
        render-options
        {:render-options {:name (project-name name)
                          :project-ns (sanitize-ns name)
                          :sanitized (name-to-path name)}}]
    (-> (merge args render-options)
        add-database-layer)))

;;;;;;play
;; (make)
;; (->files {:name "this-and-here"} ["ovde/je/ovo.clj" "Hello World!"])
;; (defn resource [r]
;;   (->> r (str "larva/frameworks/luminus/assets/core/resources/") (io/resource)))
;;;;;;

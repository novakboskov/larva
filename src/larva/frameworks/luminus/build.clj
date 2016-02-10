(ns larva.frameworks.luminus.build
  (:require [larva.code-gen.common :refer [render-assets]]
            [larva.db.utils :as db]
            [larva.program-api :as api]
            [leiningen.new.templates :refer [name-to-path project-name sanitize-ns]]
            [larva.frameworks.luminus.stuff :as stuff]))

(defn- build-api-args-map
  [{:keys [model-path model]}]
  (cond model-path {:model-path model-path}
        model      {:model model}
        :else      nil))

(defn add-database-layer
  [options]
  (let [args     (build-api-args-map options)
        entities (if args (api/all-entities args) (api/all-entities))
        db-type  (if args (db/infer-db-type args) (db/infer-db-type))]
    (doseq [entity entities]
      (let [props (if args (api/entity-properties entity args)
                      (api/entity-properties entity))
            db-options
            {:entity             (db/drill-out-name-for-db entity)
             :entity-plural      (db/build-plural-for-entity entity args)
             :properties         (db/build-sequence-string props db-type :insert)
             :values-properties  (db/build-sequence-string props db-type :values)
             :set-properties     (db/build-sequence-string props db-type :set)
             :props-create-table (db/build-sequence-string props db-type
                                                           :create-table)}]
        ;; TODO: generate files, track namespaces which are changed, those need to be reloaded later.
        ;; Better solutions is to make a macro which evaluates code that produce SQL queries from .sql files in corresponding namespace
        ;; then to produce complete new file resources/templates/frameworks/luminus/larva-specific/db/src/sql.db.clj
        ;; maybe user wants to add some more code in this file, model refreshing should not affect that code.
        (render-assets [(:queries (stuff/relational-db-files))]
                       (merge db-options (:render-options options)))))))

(defn make
  "Generate Luminus project from larva meta-model pointed to by path."
  [& {:keys [model-path model] :as args}]
  (let [name (api/project-name)
        render-options
        {:render-options {:name       (project-name name)
                          :project-ns (sanitize-ns name)
                          :sanitized  (name-to-path name)}}]
    (-> (merge args render-options)
        add-database-layer)))

;;;;;;play
;; (make :model larva.test-data/standard-program-with-meta)
;;;;;;

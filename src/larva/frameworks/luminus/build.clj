(ns larva.frameworks.luminus.build
  (:require [larva.code-gen.common :refer [render-assets]]
            [larva.db
             [tables :as tbl]
             [utils :as db]]
            [larva.frameworks.luminus.stuff :as stuff]
            [larva.program-api :as api]
            [leiningen.new.templates :refer [name-to-path project-name sanitize-ns]]))

(defn- build-api-args-map
  [{:keys [model-path model]}]
  (cond model-path {:model-path model-path}
        model      {:model model}
        :else      nil))

(defn- add-additional
  [references templates options]
  (let [render-options (:render-options templates) force (:force options)
        args           (build-api-args-map options)
        ;; db-options
        ;; {:alter-tables (tbl/build-alter-tables-strings references)}
        ]
    ;; TODO:
    (doseq [k (tbl/build-additional-templates-keys references args options)]
      (render-assets [:additional-migrations-sql-up templates]
                     (merge k render-options)))
    ;; (render-assets [(:additional-migrations-sql-up templates)
    ;;                 (:migtrations-alter-up templates)
    ;;                 (:additional-queries templates)
    ;;                 (:additional-migrations-sql-down templates)]
    ;;                (merge db-options render-options))
    ))

(defn add-database-layer
  [options]
  (let [args      (build-api-args-map options)
        force     (:force options)
        entities  (if args (api/all-entities args) (api/all-entities))
        db-type   (if args (db/infer-db-type args) (db/infer-db-type))
        templates (stuff/relational-db-files)]
    (loop [ents entities references []]
      (if (not-empty ents)
        (let [entity      (nth ents 0)
              props       (if args (api/entity-properties entity args)
                              (api/entity-properties entity))
              ent-db-name (db/drill-out-name-for-db entity)
              [props-create-table refs]
              (tbl/build-db-create-table-string entity props db-type force args)
              db-options
              {:entity             ent-db-name
               :entity-plural      (db/build-plural-for-entity entity args)
               :properties         (db/build-sequence-string props db-type :insert)
               :values-properties  (db/build-sequence-string props db-type :values)
               :set-properties     (db/build-sequence-string props db-type :set)
               :props-create-table props-create-table}]
          (render-assets [(:migrations-sql-up templates)
                          (:queries templates)
                          (:migrations-sql-down templates)]
                         (merge db-options (:render-options options)))
          (recur (rest ents) (conj references refs)))
        (add-additional references templates db-type force options)))))

;; TODO: generate files, track namespaces which are changed, those need to be reloaded later.
;; Better solutions is to make a macro which evaluates code that produce SQL queries from .sql files in corresponding namespace
;; then to produce complete new file resources/templates/frameworks/luminus/larva-specific/db/src/sql.db.clj
;; maybe user wants to add some more code in this file, model refreshing should not affect that code.

(defn make
  "Generate Luminus project from larva meta-model pointed to by path."
  [& {:keys [model-path model force] :as args}]
  (let [name (api/project-name)
        render-options
        {:render-options {:name       (project-name name)
                          :project-ns (sanitize-ns name)
                          :sanitized  (name-to-path name)}}]
    (-> (merge args render-options)
        add-database-layer)))

;;;;;;play
;; (make :model larva.test-data/custom-property-datatype :force true)
;;;;;;

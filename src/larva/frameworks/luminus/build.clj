(ns larva.frameworks.luminus.build
  (:require [clojure.java.io :as io]
            [larva.code-gen.common :refer [render-assets]]
            [larva.db
             [tables :as tbl]
             [utils :as db]]
            [larva.frameworks.luminus.stuff :as stuff]
            [larva.program-api :as api]
            [leiningen.new.templates :refer [name-to-path project-name
                                             sanitize-ns]]))

(def default-sql-tool :hugsql)

(defmacro clean-field
  "Delete files and directories which are touched by larva if it's needed
   and evaluates passed forms in that clean context."
  [make-args & forms]
  `(do (cond (or (nil? ~make-args) (get-in ~make-args [:force]))
             (do (doseq [migration-files#
                         (.listFiles (io/file stuff/migrations-dir))]
                   (io/delete-file migration-files#))
                 (doseq [queries-files#
                         (.listFiles (io/file stuff/queries-dir))]
                   (io/delete-file queries-files#))))
       ~@forms))

(defn- build-api-args-map
  [{:keys [model-path model]}]
  (cond model-path {:model-path model-path}
        model      {:model model}
        :else      nil))

(defn- build-sql-tool [args]
  (or
   (get-in (if args (api/program-meta args) (api/program-meta)) [:db :type])
   default-sql-tool))

(defn- add-additional
  [references templates db-type force sql-tool args options]
  (let [render-options (:render-options options)
        ks             (tbl/build-additional-templates-keys references args)]
    (doseq [create-tables (:create-tables ks)]
      (let [additional-migrations-sql ((:additional-migrations-sql templates))]
        (render-assets [;; create table
                        (:up additional-migrations-sql)
                        ;; drop table
                        (:down additional-migrations-sql)]
                       (merge create-tables render-options))))
    (let [migrations-alter ((:migrations-alter templates))]
      ;; then make all the alters in single file
      (render-assets [(:up migrations-alter)]
                     (merge ks render-options))
      ;; then make all the alter drops in single file
      (render-assets [(:down migrations-alter)]
                     (merge ks render-options)))
    ;; then make all the additional queries
    (render-assets [((:additional-queries templates) sql-tool)]
                   (merge ks render-options))))

(defn add-database-layer
  [options]
  (let [args     (build-api-args-map options)
        sql-tool (build-sql-tool args)
        force    (:force options)
        entities (if args (api/all-entities args) (api/all-entities))
        db-type  (if args (db/infer-db-type args) (db/infer-db-type))]
    (loop [ents entities references []]
      (if (not-empty ents)
        (let [entity         (nth ents 0)
              props          (if args (api/entity-properties entity args)
                                 (api/entity-properties entity))
              [props-create-table refs]
              (tbl/build-db-create-table-string entity props db-type force args)
              db-options
              {:entity             (db/drill-out-name-for-clojure entity)
               :entity-plural      (db/build-db-table-name entity args)
               :properties         (db/build-sequence-string props db-type :insert)
               :values-properties  (db/build-sequence-string props db-type :values)
               :set-properties     (db/build-sequence-string props db-type :set)
               :props-create-table props-create-table}
              migrations-sql ((:migrations-sql stuff/relational-db-files))]
          (render-assets [(:up migrations-sql)
                          ((:queries stuff/relational-db-files) sql-tool)
                          (:down migrations-sql)]
                         (merge db-options (:render-options options)))
          (recur (rest ents) (conj references refs)))
        (add-additional references stuff/relational-db-files
                        db-type force sql-tool args options)))))

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
    (clean-field args
                 (-> (merge args render-options)
                     add-database-layer))))

;;;;;; play
;; (make :model larva.test-data/custom-property-datatype :force true)
;; (make)
;;;;;;

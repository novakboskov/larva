(ns larva.frameworks.luminus.stuff
  "All variables and building material specific to a Luminus project."
  (:require [clojure.java.io :refer [resource]]))

(def ^:private queries-templates-dir "db/sql/")
(def ^:dynamic queries-dir "resources/sql/")
(def ^:private migrations-templates-dir "db/migrations/")
(def ^:dynamic migrations-dir "resources/migrations/")

(def core-assets
  [[".gitignore" "core/gitignore"]
   ["project.clj" "core/project.clj"]
   ["profiles.clj" "core/profiles.clj"]
   ["Procfile" "core/Procfile"]
   ["README.md" "core/README.md"]
   ["env/prod/resources/config.edn" "core/env/prod/resources/config.edn"]

   ;; config namespaces
   ["env/dev/clj/{{sanitized}}/config.clj" "core/env/dev/clj/config.clj"]
   ["env/dev/clj/{{sanitized}}/dev_middleware.clj" "core/env/dev/clj/dev_middleware.clj"]
   ["env/prod/clj/{{sanitized}}/config.clj" "core/env/prod/clj/config.clj"]
   ;; core namespaces
   ["env/dev/clj/user.clj" "core/env/dev/clj/user.clj"]
   ["src/{{sanitized}}/core.clj" "core/src/core.clj"]
   ["src/{{sanitized}}/handler.clj" "core/src/handler.clj"]
   ["src/{{sanitized}}/routes/home.clj" "core/src/home.clj"]
   ["src/{{sanitized}}/layout.clj" "core/src/layout.clj"]
   ["src/{{sanitized}}/middleware.clj" "core/src/middleware.clj"]

   ;; HTML templates
   ["resources/templates/base.html" "core/resources/templates/base.html"]
   ["resources/templates/home.html" "core/resources/templates/home.html"]
   ["resources/templates/about.html" "core/resources/templates/about.html"]
   ["resources/templates/error.html" "core/resources/templates/error.html"]

   ;; public resources, example URL: /css/screen.css
   ["resources/public/favicon.ico"  (resource "site/favicon.ico")]
   ["resources/public/css/screen.css" "core/resources/css/screen.css"]
   ["resources/docs/docs.md" "core/resources/docs.md"]
   "resources/public/js"
   "resources/public/img"

   ;; tests
   ["test/{{sanitized}}/test/handler.clj" "core/test/handler.clj"]])

(defn- get-migration-timestamp []
  ;; TODO: This should always return timestamp string of fixed length
  (.format (java.text.SimpleDateFormat. "yyyyMMddHHmmssSS")
           (java.util.Date.)))

(defn- relational-db-queries [additional-or-not key]
  (let [[q-file-name q-template] (case additional-or-not
                                   :additional ["additional_queries.sql"
                                                (case key
                                                  :yesql "add-queries_yesql.sql"
                                                  "add-queries_hugsql.sql")]
                                   ["{{entity-plural}}_queries.sql"
                                    (case key
                                      :yesql "queries_yesql.sql"
                                      "queries_hugsql.sql")])]
    [(str queries-dir q-file-name)
     (str queries-templates-dir q-template)]))

(def relational-db-files
  {:core               ["src/clj/{{sanitized}}/db/core.clj" "db/src/sql.db.clj"]
   :migrations-clj     ["src/clj/{{sanitized}}/db/migrations.clj" "db/src/migrations.clj"]
   :queries            (partial relational-db-queries false)
   :additional-queries (partial relational-db-queries :additional)
   :core-test          ["test/clj/{{sanitized}}/test/db/core.clj" "db/test/db/core.clj"]
   :migrations-sql
   (fn [] (let [timestamp (get-migration-timestamp)]
            {:up   [(str migrations-dir timestamp
                         "-add-{{entity-plural}}-table.up.sql")
                    (str migrations-templates-dir "add-entity-table.up.sql")]
             :down [(str migrations-dir timestamp
                         "-add-{{entity-plural}}-table.down.sql")
                    (str migrations-templates-dir "add-entity-table.down.sql")]}))
   :migrations-alter
   (fn [] (let [timestamp (get-migration-timestamp)]
            {:up   [(str migrations-dir timestamp
                         "-alter-tables.up.sql")
                    (str migrations-templates-dir "add-alter-tables.up.sql")]
             :down [(str migrations-dir timestamp
                         "-alter-tables.down.sql")
                    (str migrations-templates-dir "add-alter-tables.down.sql")]}))
   :additional-migrations-sql
   (fn [] (let [timestamp (get-migration-timestamp)]
            {:up   [(str migrations-dir timestamp
                         "-add-{{ad-entity-plural}}-table.up.sql")
                    (str migrations-templates-dir
                         "add-additional-entity-table.up.sql")]
             :down [(str migrations-dir timestamp
                         "-add-{{ad-entity-plural}}-table.down.sql")
                    (str migrations-templates-dir
                         "add-additional-entity-table.down.sql")]}))})

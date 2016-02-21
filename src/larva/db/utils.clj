(ns larva.db.utils
  "Provides common utilities which larva needs for producing database related things."
  (:require [clojure.java.io :as io]
            [clojure.string :as cs]
            [conman.core :as conman]
            [larva
             [messages :as msg]
             [program-api :as api]
             [program-api-schemes :as sch]
             [utils :as utils]]
            [larva.db.stuff :as stuff :refer :all]
            [yesql.core :refer [defqueries]]))

(def objects (atom {}))
(def default-db-data-types-config
  (io/file api/default-larva-dir "db_types_config.clj"))

(defn infer-db-type
  "If :db key exists in :meta section of larva model it will be returned,
  otherwise type of used database will be inferred from database drivers provided
  in project.clj."
  [& args]
  (or (:db (if args (apply api/program-meta args) (api/program-meta)))
      (let [deps     (map #(str (first %)) (:dependencies
                                            (utils/make-project-clj-map)))
            matcher  #(re-matches (re-pattern (str "^.*" %1 ".*$")) %2)
            db-types (reduce
                      #(cond (matcher "postgres" %2) (conj %1 :postgres)
                             (matcher "mysql" %2)    (conj %1 :mysql)
                             (matcher "h2" %2)       (conj %1 :h2)
                             (matcher "sqlite" %2)   (conj %1 :sqlite)
                             (matcher "mongo" %2)    (conj %1 :mongo)) [] deps)
            selected (first db-types)]
        (cond (= (count db-types) 0)
              (msg/warn (get-in msg/messages [:db-infer :no-infer]))
              (> (count db-types) 1)
              (do (msg/warn
                   ((get-in msg/messages [:db-infer :multi-dbs-inferred])
                    db-types selected)) selected)
              :default selected))))

(defn symbol-uuid []
  (java.util.UUID/randomUUID))

(defmacro wrap-object [object symbol]
  (swap! objects #(assoc % symbol object))
  `(let [o# (~symbol @objects)]
     (swap! objects #(dissoc % ~symbol))
     o#))

(defn- make-code-that-evals-to [object]
  (let [symbol (keyword (str (symbol-uuid)))]
    `(wrap-object ~object ~symbol)))

(defmacro ^:private functionalize [macro]
  `(fn [conn# & filenames#] (let [code# (make-code-that-evals-to conn#)]
                              (eval (cons '~macro (cons code# filenames#))))))

(defn make-queries-from-dirs
  "Takes a variable number of directory paths (which are presumably in the classpath),
  options to forward to `yesql.core/defqueries` or connection to forward to `conman.core/bind-connection`  as arguments.
  If options are provided:
  for each directory path, finds .sql files in it and calls `defqueries` on each.
  If connection is provided:
  Make vector of .sql file paths contained in provided directories and bind connection for each directory.
  Does not walk subdirectories for more .sql files.
  Returns vector of queries partitioned by directories they originated from."
  [{:keys [paths options connection]}]
  (loop [dir-paths paths queries []]
    (if (not-empty dir-paths)
      (let [dir  (first dir-paths)
            sqls (for [file (file-seq (-> dir io/resource io/file))
                       :let [name (.getName file)]
                       :when
                       (#(and (.isFile %) (re-matches #".*\.sql$" name)) file)]
                   (str dir (System/getProperty "file.separator") name))]
        (recur (rest dir-paths)
               (if (nil? connection)
                 (loop [files sqls q []]
                   (if (not-empty files)
                     (recur (rest files)
                            (conj q (defqueries (first files) options)))
                     (conj queries (flatten q))))
                 (conj queries (apply (functionalize conman/bind-connection)
                                      connection sqls)))))
      queries)))

(defn drill-out-name-for-db [name]
  (-> name (cs/replace #"[^a-zA-Z0-9]" "_") cs/lower-case))

(defmulti build-sequence-string
  "Builds string suited for INSERT, CREATE TABLE or VALUES SQL statement.
  What can be either :insert, :create-table, :values or :set."
  (fn [properties db-type what] what))

(defmethod build-sequence-string :insert
  [properties _ _]
  (str "("
       (cs/replace-first
        (reduce #(->> (drill-out-name-for-db (:name %2)) (str ":") (str ", ")
                      (str %1)) "" properties)
        ", " "")
       ")"))

(defmethod build-sequence-string :values
  [properties _ _]
  (str "("
       (cs/replace-first
        (reduce #(->> (drill-out-name-for-db (:name %2)) (str ", ")
                      (str %1)) "" properties)
        ", " "")
       ")"))

(defmethod build-sequence-string :set
  [properties _ _]
  (cs/replace-first
   (reduce #(let [name (drill-out-name-for-db (:name %2))]
              (->> (str name " = :" name) (str ", ")
                   (str %1))) "" properties)
   ", " ""))

(defn make-db-data-types-config
  "Makes database types configuration file if it is not present.
  It can receive map containing :model or :model-path as :spec key.
  Returns that configuration."
  [& {:keys [spec db-type force]}]
  (if (or (not (.exists default-db-data-types-config)) force)
    (let [db-type (or db-type (if spec (infer-db-type spec) (infer-db-type)))]
      (utils/spit-data default-db-data-types-config
                       (or (db-type database-types-config) {}))))
  (utils/slurp-as-data default-db-data-types-config))

(defn build-plural-for-entity
  "If program specifies entity plural it will be return, otherwise it will be
  constructed using suffix 's'."
  [entity-signature model-source]
  (let [entity (api/entity-info entity-signature model-source)]
    (if-let [plural (:plural entity)] (drill-out-name-for-db plural)
            (str (drill-out-name-for-db entity-signature) "s"))))

(defn build-db-table-name
  "Builds DB table name from plural if model-source is present or from bare name
  if it's not."
  [entity-signature & [model-source singular]]
  (let [drilled-name (drill-out-name-for-db entity-signature)
        to-capt      (if-not singular
                       (build-plural-for-entity entity-signature model-source)
                       drilled-name)]
    (if model-source
      (cs/capitalize to-capt)
      (cs/capitalize drilled-name))))

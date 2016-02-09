(ns larva.db.utils
  "Provides common utilities which larva needs for producing database related things."
  (:require [clojure.java.io :as io]
            [clojure.string :as cs]
            [conman.core :as conman]
            [yesql.core :refer [defqueries]]
            [larva.program-api :as api]))

(def objects (atom {}))

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
    (if (-> dir-paths count (> 0))
      (let [dir  (first dir-paths)
            sqls (for [file (file-seq (-> dir io/resource io/file))
                       :let [name (.getName file)]
                       :when
                       (#(and (.isFile %) (re-matches #".*\.sql$" name)) file)]
                   (str dir (System/getProperty "file.separator") name))]
        (recur (rest dir-paths)
               (if (nil? connection)
                 (loop [files sqls q []]
                   (if (-> files count (> 0))
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
  (fn [properties what] what))

(defmethod build-sequence-string :insert
  [properties _]
  (str "("
       (cs/replace-first
        (reduce #(->> (drill-out-name-for-db (:name %2)) (str ":") (str ", ")
                      (str %1)) "" properties)
        ", " "")
       ")"))

(defmethod build-sequence-string :values
  [properties _]
  (str "("
       (cs/replace-first
        (reduce #(->> (drill-out-name-for-db (:name %2)) (str ", ")
                      (str %1)) "" properties)
        ", " "")
       ")"))

(defmethod build-sequence-string :set
  [properties _]
  (cs/replace-first
   (reduce #(let [name (drill-out-name-for-db (:name %2))]
              (->> (str name " = :" name) (str ", ")
                   (str %1))) "" properties)
   ", " ""))

(defn build-plural-for-entity
  "If program specifies entity plural it will be return, otherwise it will be
  constructed using suffix 's'."
  [entity-signature model-source]
  (let [entity (api/entity-info entity-signature model-source)]
    (if-let [plural (:plural entity)] (drill-out-name-for-db plural)
            (str (drill-out-name-for-db entity-signature) "s"))))

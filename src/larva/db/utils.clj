(ns larva.db.utils
  "Provides common utilities which larva needs for producing database related things."
  (:require [clojure.java.io :as io]
            [yesql.core :refer [defqueries]]
            [conman.core :as conman]))

(def ^:private objects (atom {}))

(defn- symbol-uuid []
  (java.util.UUID/randomUUID))

(defmacro ^:private wrap-object [object symbol]
  (swap! objects #(assoc % symbol object))
  `(let [o# (~symbol objects)]
     (swap! objects #(dissoc % ~symbol))
     o#))

(defn- make-code-that-evals-to [object]
  (let [symbol (keyword (str (symbol-uuid)))]
    `(wrap-object ~object ~symbol)))

(defmacro ^:private functionalize [macro]
  `(fn [conn# & filenames#] (let [code# ('make-code-that-evals-to conn#)]
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
      (let [dir (first dir-paths)
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

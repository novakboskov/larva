(ns larva.db.utils
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
  options to forward to yesql.core/defqueries or connection to forward to conman.core/bind-connection  as arguments.
  If options are provided:
  for each path, finds the .sql files and calls `defqueries` on each.
  If connection is provided:
  Make vector of sql paths contained in provided directories and bind connection for each directory.
  Does not walk subdirectories for more .sql files."
  [{:keys [paths options connection]}]
  (loop [dir-paths paths queries []]
    (if (> (count dir-paths) 0)
      (do
        (let [dir (first dir-paths)
              sqls (for [file (file-seq (-> dir io/resource io/file))
                         :let [name (.getName file)]
                         :when
                         (#(and (.isFile %) (re-matches #".*\.sql$" name)) file)]
                     (str dir (System/getProperty "file.separator") name))]
          (if (nil? connection)
            (loop [files sqls queries []]
              (if (> (count files) 0)
                (recur (rest files)
                       (conj queries (defqueries (first files) options)))
                queries))
            (recur (rest dir-paths)
                   (apply (functionalize conman/bind-connection)
                          connection sqls)))))
      queries)))

(ns larva.db.utils
  (:require [clojure.java.io :as io]
            [conman.core :as cnm]
            [yesql.core :refer [defqueries]]))

(defn make-queries-from-dirs
  "Takes a variable number of directory paths (which are presumably in the classpath)
  and options to forward to yesql.core/defqueries as arguments.
  For each path, finds the .sql files and calls `defqueries` on each.
  Does not walk subdirectories for more .sql files."
  [{:keys [paths options]}]
  (loop [dir-paths paths]
    (if (> (count dir-paths) 0)
      (do
        (let [dir (first dir-paths)
              sqls (for [file (file-seq (-> dir io/resource io/file))
                         :let [name (.getName file)]
                         :when
                         (#(and (.isFile %) (re-matches #".*\.sql$" name)) file)]
                     (str dir (System/getProperty "file.separator") name))]
          (loop [files sqls]
            (if (> (count files) 0)
              (do
                (defqueries (first files) options)
                (recur (rest files))))))
        (recur (rest dir-paths))))))

(defn bind-connections-from-dirs
  "Takes a variable number of directory paths (which are presumably in the classpath)
  and a connection. All the sql files in specified directories will be packed
  in a vector and alongside connection passed to conman/bind-connection.
  Does not walk subdirectories for more .sql files."
  [connection paths])

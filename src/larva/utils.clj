(ns larva.utils
  (:require [clojure.edn :as edn]))

(defn parse-project-clj
  []
  (try
    (-> (slurp "project.clj") edn/read-string)
    (catch Exception e (str "project.clj is not fund in root of your project: "
                            (.getMessage e)))))

(defn make-project-clj-map
  "Reads project.clj from root of project and coerce it into map."
  []
  (let [p (parse-project-clj)]
    (merge {:name    (str (nth p 1))
            :version (str (nth p 2))}
           (into {} (map vec (partition 2 (drop 3 p)))))))

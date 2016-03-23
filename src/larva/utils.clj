(ns larva.utils
  (:require [clojure
             [edn :as edn]
             [pprint :as pp]]
            [clojure.java.io :as io]
            [schema.core :as s]))

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

(defn slurp-as-data [file]
  (edn/read-string (slurp file)))

(defn spit-data [file content]
  (io/make-parents file)
  (spit file (with-out-str (pp/pprint content))))

(defn valid?
  "Checks if data satisfies schema. If it satisfies returns data otherwise false."
  [schema data]
  (try
    (s/validate schema data)
    (catch Exception e false)))

(defn swap-keys
  "Swaps values of two keys in map"
  [map k1 k2]
  (let [f (get map k1) s (get map k2)]
    (assoc map k1 s k2 f)))

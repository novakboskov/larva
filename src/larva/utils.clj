(ns larva.utils
  (:require [clojure.edn :as edn]))

(defn parse-project-clj
  [project]
  (-> project edn/read-string (nth 1) str))

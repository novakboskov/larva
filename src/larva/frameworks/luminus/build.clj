(ns larva.frameworks.luminus.build
  (:require [clojure.java.io :as io]
            [larva.model-utils :refer [model->program]]
            [leiningen.new.templates :refer [->files]]))

(defn make [& {:keys [path]}]
  "Generate Luminus project from larva meta-model pointed to by path."
  (let [program (if (nil? path) (model->program) (model->program {:path path}))]
    ))

(->files {:name "this-and-here"} ["ovde/je/ovo.clj" "Hello World!"])

(defn resource [r]
  (->> r (str "larva/frameworks/luminus/assets/core/resources/") (io/resource)))

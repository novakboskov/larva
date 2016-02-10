(ns larva.messages)

(def messages
  {:db-infer
   {:no-infer
    (str
     "Database type you use could not be inferred from your project.clj." (System/lineSeparator)
     "Please povide either :postgres, :mysql, :h2, :sqlite or :mongo as a :db in meta section of your larva model.")
    :multi-dbs-inferred
    #(str "Multiple database drivers are found in your project.clj: " %1 (System/lineSeparator)
          "The first one (" %2 ") will be used.")}})

(defn warn
  [& args]
  (binding [*out* *err*]
    (print (apply str "Warning: " args))))

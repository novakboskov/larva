(ns larva.test-results)

(def results
  {:build-additional-templates-keys-test
   [{:create-tables
     [{:ad-entity-plural "Musicians__honors__smpl_coll",
       :ad-props-create-table
       "(id SERIAL PRIMARY KEY,\n musicians_id INTEGER REFERENCES Musicians(id),\n honors VARCHAR(30))"}],
     :queries
     [{:ent       "musician"
       :prop      "honors"
       :f-tbl     "Musicians__honors__smpl_coll"
       :f-id      "musician_id"
       :sign      "="
       :no-nest   true
       :sel-multi true}
      {:assoc true
       :ent   "musician"
       :prop  "honors"
       :f-tbl "Musicians__honors__smpl_coll"
       :f-id  "musician_id"
       :s-id  "honors"
       :insert-values
       "\n/*~\n(let [single (:musician params)]\n    (clojure.string/join\n        \", \" (for [m (:honors params)]\"(\" single \", \" m \")\")))\n~*/"}
      {:ent         "musician"
       :prop        "honors"
       :f-tbl       "Musicians__honors__smpl_coll"
       :f-id        "musician_id"
       :s-id        "honors"
       :insert-values
       "\n/*~\n(let [single (:musician params)]\n    (clojure.string/join\n        \", \" (for [m (:honors params)]\"(\" single \", \" m \")\")))\n~*/",
       :dissoc      true
       :reverse-doc true}
      {:ent         "musician"
       :prop        "honors"
       :f-tbl       "Musicians__honors__smpl_coll"
       :f-id        "musician_id"
       :s-id        "honors"
       :insert-values
       "\n/*~\n(let [single (:musician params)]\n    (clojure.string/join\n        \", \" (for [m (:honors params)]\"(\" single \", \" m \")\")))\n~*/",
       :dissoc-all  true
       :reverse-doc true}]}]})

(ns larva.db.stuff
  (:require [clojure.string :as cs]))

(def database-types-config
  {:postgres
   {:str      "VARCHAR(30)" :text   "TEXT"
    :num      "INTEGER"     :bignum "BIGINT" :float     "FLOAT4" :bigfloat "FLOAT8"
    :datetime "TIMESTAMPTZ" :date   "DATE"   :timestamp "TIMESTAMPT"
    :bool     "BOOLEAN"
    :geo      "POINT"
    :json     "JSON"
    :binary   "BYTEA"
    :pass     "VARCHAR(300)"
    :id       "SERIAL"}
   :mysql
   {:str      "VARCHAR(30)" :text   "TEXT"
    :num      "INTEGER"     :bignum "BIGINT" :float     "FLOAT4" :bigfloat "DOUBLE"
    :datetime "DATETIME"    :date   "DATE"   :timestamp "TIMESTAMP"
    :bool     "BIT"
    :geo      "POINT"
    :json     "JSON"
    :binary   "BINARY"
    :pass     "VARCHAR(300)"
    :id       "INTEGER AUTO_INCREMENT"}
   :h2
   {:str      "VARCHAR(30)" :text   "TEXT"
    :num      "INTEGER"     :bignum "BIGINT" :float     "FLOAT" :bigfloat "DOUBLE"
    :datetime "TIMESTAMP"   :date   "DATE"   :timestamp "TIMESTAMP"
    :bool     "BOOLEAN"
    :geo      "VARCHAR(30)"
    :json     "VARCHAR(300)"
    :binary   "BINARY"
    :pass     "VARCHAR(300)"
    :id       "IDENTITY"}
   :sqlite
   {}
   :mongodb
   {}})

(defn- simple-colection-table-columns [prim-key db-types column]
  (str " id " (:id db-types) " " prim-key "," (System/lineSeparator)
       " " (nth column 0) " " (:num db-types) " REFERENCES "
       (nth column 1) "(" (nth column 2)  ")," (System/lineSeparator)
       " " (nth column 3) " " ((nth column 4) db-types)))

(defn mysql-referential-table-columns
  ([prim-key db-types first second]
   (let [column #(str (nth % 0) " " (:num db-types) " REFERENCES "
                      (nth % 1) "(" (nth % 2)  ")")]
     (str " id " (:id db-types) " " prim-key ","
          (System/lineSeparator)
          " " (column first) "," (System/lineSeparator) " " (column second)
          (let [uniqs (for [c [first second] :when (nth c 3)] (nth c 0))]
            (if (not-empty uniqs) (str "," (System/lineSeparator)
                                       " UNIQUE(" (cs/join ", " uniqs) ")")
                "")))))
  ([prim-key db-types column]
   (simple-colection-table-columns prim-key db-types column)))

(defn psql-referential-table-columns
  ([prim-key db-types first second]
   (let [column #(str (nth % 0) " " (:num db-types) " REFERENCES "
                      (nth % 1) "(" (nth % 2)  ")"
                      (if (nth % 3) " UNIQUE" ""))]
     (str " id " (:id db-types) " " prim-key ","
          (System/lineSeparator)
          " " (column first) "," (System/lineSeparator) " " (column second))))
  ([prim-key db-types column]
   (simple-colection-table-columns prim-key db-types column)))

(def database-grammar
  {:postgres
   (let [prim-key "PRIMARY KEY"]
     {:not-null "NOT NULL" :prim-key prim-key
      :referential-table-columns
      (partial psql-referential-table-columns prim-key)})
   :mysql
   (let [prim-key "PRIMARY KEY"]
     {:not-null "NOT NULL" :prim-key prim-key
      :referential-table-columns
      (partial mysql-referential-table-columns prim-key)})
   :h2
   (let [prim-key "PRIMARY KEY"]
     {:not-null "NOT NULL" :prim-key prim-key
      :references
      (partial mysql-referential-table-columns prim-key)})
   :sqlite
   {}
   :mongodb
   {}})

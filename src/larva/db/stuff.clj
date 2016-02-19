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
    :id       "serial"}
   :mysql
   {:str      "VARCHAR(30)" :text   "TEXT"
    :num      "INTEGER"     :bignum "BIGINT" :float     "FLOAT4" :bigfloat "DOUBLE"
    :datetime "DATETIME"    :date   "DATE"   :timestamp "TIMESTAMP"
    :bool     "BIT"
    :geo      "POINT"
    :json     "JSON"
    :binary   "BINARY"
    :pass     "VARCHAR(300)"
    :id       "AUTO_INCREMENT"}
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

(defn- mysql-referential-table-columns
  [db-types first second]
  (let [column #(str (nth % 0) " " (:num db-types) " REFERENCES "
                     (nth % 1) "(" (nth % 2)  ")")]
    (str "id " (:id db-types) " " (:prim-key (db-types database-grammar)) ","
         (System/lineSeparator)
         (column first) "," (System/lineSeparator) (column second)
         (let [uniqs (for [c [first second] :when (nth c 3)] (nth c 0))]
           (if (not-empty uniqs) (str "," (System/lineSeparator)
                                      "UNIQUE(" (cs/join ", " uniqs) ")")
               "")))))

(defn- psql-referential-table-columns
  [db-types first second]
  (let [column #(str (nth % 0) " " (:num db-types) " REFERENCES "
                     (nth % 1) "(" (nth % 2)  ")"
                     (if (nth 3 %) " UNIQUE" ""))]
    (str "id " (:id db-types) " " (:prim-key (db-types database-grammar)) ","
         (System/lineSeparator)
         (column first) "," (System/lineSeparator) (column second))))

(def database-grammar
  {:postgres
   {:not-null                  "NOT NULL" :prim-key "PRIMARY KEY"
    :referential-table-columns psql-referential-table-columns}
   :mysql
   {:not-null                  "NOT NULL" :prim-key "PRIMARY KEY"
    :referential-table-columns mysql-referential-table-columns}
   :h2
   {:not-null   "NOT NULL" :prim-key "PRIMARY KEY"
    :references #(str "REFERENCES " %1 "(" %2 ")")}
   :sqlite
   {}
   :mongodb
   {}})

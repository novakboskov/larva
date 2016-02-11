(ns larva.db.stuff)

(def database-types-config
  {:postgres
   {:str      "VARCHAR(30)" :text   "TEXT"
    :num      "INTEGER"     :bignum "BIGINT" :float     "FLOAT4" :bigfloat "FLOAT8"
    :datetime "TIMESTAMPTZ" :date   "DATE"   :timestamp "TIMESTAMPT"
    :bool     "BOOLEAN"
    :geo      "POINT"
    :json     "JSON"
    :binary   "BYTEA"
    :pass     "VARCHAR(300)"}
   :mysql
   {:str      "VARCHAR(30)" :text   "TEXT"
    :num      "INTEGER"     :bignum "BIGINT" :float     "FLOAT4" :bigfloat "DOUBLE"
    :datetime "DATETIME"    :date   "DATE"   :timestamp "TIMESTAMP"
    :bool     "BIT"
    :geo      "POINT"
    :json     "JSON"
    :binary   "BINARY"
    :pass     "VARCHAR(300)"}
   :h2
   {:str      "VARCHAR(30)" :text   "TEXT"
    :num      "INTEGER"     :bignum "BIGINT" :float     "FLOAT" :bigfloat "DOUBLE"
    :datetime "TIMESTAMP"   :date   "DATE"   :timestamp "TIMESTAMP"
    :bool     "BOOLEAN"
    :geo      "VARCHAR(30)"
    :json     "VARCHAR(300)"
    :binary   "BINARY"
    :pass     "VARCHAR(300)"}
   :sqlite
   {}
   :mongodb
   {}})

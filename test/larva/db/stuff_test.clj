(ns larva.db.stuff-test
  (:require [larva.db.stuff :as stuff]
            [clojure.test :refer :all]))

(deftest psql-referential-table-columns-test
  (testing "Returned string."
    (let [db-type  :postgres
          db-types (db-type stuff/database-types-config)
          grammar  (db-type stuff/database-grammar)]
      (is (= " id SERIAL PRIMARY KEY,\n some_name INTEGER REFERENCES Some_table(id) UNIQUE,\n some_name2 INTEGER REFERENCES Some_table2(id) UNIQUE"
             (stuff/psql-referential-table-columns
              (:prim-key grammar) db-types ["some_name" "Some_table" "id" true]
              ["some_name2" "Some_table2" "id" true]))))
    (let [db-type  :mysql
          db-types (db-type stuff/database-types-config)
          grammar  (db-type stuff/database-grammar)]
      (is (= " id INTEGER AUTO_INCREMENT PRIMARY KEY,\n some_name INTEGER REFERENCES Some_table(id),\n some_name2 INTEGER REFERENCES Some_table2(id),\n UNIQUE(some_name, some_name2)"
             (stuff/mysql-referential-table-columns
              (:prim-key grammar) db-types ["some_name" "Some_table" "id" true]
              ["some_name2" "Some_table2" "id" true]))))))

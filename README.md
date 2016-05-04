[![Circle CI](https://circleci.com/gh/novakboskov/larva.svg?style=svg)](https://circleci.com/gh/novakboskov/larva)
[![Clojars Project](https://img.shields.io/clojars/v/larva.svg)](https://clojars.org/larva)

# larva

Tends to be a Clojure application generator.

# What larva does?

Larva tends to become an easier way to build applications in Clojure and
provides mechanisms for rapid development on solid foundations.

The main emphasis is on building WEB applications and services.

# What larva is?

Larva is a software package containing a tiny DSL, code generators and other
utilities.

It is divided into two parts:

* ```larva.db``` (provides db utilities)
* ```larva.web``` (provides generating web applications)

Since Clojure tooling getting more diverse Larva is striving to provide support
for best practices from Clojure community or to be tool/framework independent
where it's possible.

## larva DSL

Larva DSL is Clojure internal DSL which looks like this:

``` clojure
(def larva-model
  {:about
   {:name    "model name"
    :author  "Author"
    :comment "Comment here."}
   :entities
   [{:signature  "Entity 1"
     :properties [{:name "property 1" :type :str :gui-label "p1"}
                  {:name "property 2" :type :str :gui-label "p2"}
                  {:name "property 3" :type {:coll :str}}
                  {:name "property 4" :type {:one :reference
                                             :to  ["Entity 2"]}}]}
    {:signature  "Entity 2"
     :properties [{:name "property 1" :type :str :gui-label "p11"}
                  {:name "property 2" :type :str :gui-label "p22"}
                  {:name "property 3" :type :num :gui-label "p33"}
                  {:name      "property 4" :type {:coll :reference
                                                  :to   ["Entity 1" "property 4"]}
                   :gui-label "p44"}]}
    {:signature  "Entity 3"
     :properties [{:name "property 1" :type :str :gui-label "p111"}
                  {:name "property 2" :type :geo :gui-label "p222"}]}]})
```

Full language meta model is defined using [```plumatic/schema```](https://github.com/plumatic/schema)
and can be found in ```larva.meta-model``` namespace.

## Try it

I'm currently implementing ```larva.db``` so you can try/test it.
To do that you should make a fresh [Luminus](http://www.luminusweb.net/docs#creating_a_new_application) project with ```+postgres``` profile.

Setup your database connection using ```profiles.clj``` in the root of your
project.

Then add [larva jar](https://clojars.org/larva) as a dependency of your fresh
project:

``` clojure
;; in project.clj

:dependencies [[org.clojure/clojure "1.8.0"]
                 ...
                 [larva "0.1.0-SNAPSHOT"]]
```

Now you can fire REPL and run:

``` clojure
(require '[larva.frameworks.luminus.build :as larva])

;; This namespace contains few models for testing.
;; You can write your own model though.
(require '[larva.test-data :as larva-test])

(larva/make :model larva-test/custom-property-datatype :force true)
```
This will fill ```resources/migrations``` and ```resources/sql``` with database
migrations and [HugSQL](http://www.hugsql.org/) queries respectively.

To setup your database you should run up migratioins from
```resources/migrations``` using:

``` shell
lein run migrate
```

Now you should run ```(start)``` form ```user``` namespace in REPL to start all
the [Mount](https://github.com/tolitius/mount) states.

Then enter the ```<your application>.db.core``` namespace and run:

``` clojure
(conman/bind-connection *db*
                        "sql/additional_queries.sql"
                        "sql/Bands_queries.sql"
                        "sql/Categories_queries.sql"
                        "sql/Festivals_queries.sql"
                        "sql/Instruments_queries.sql"
                        "sql/Mentors_queries.sql"
                        "sql/More_infos_queries.sql"
                        "sql/Musicians_queries.sql"
                        "sql/Socialmediaprofiles_queries.sql")
```

This will bind all the [HugSQL](http://www.hugsql.org/) queries to database
connection.

... and your ready to play with ```larva.db```:

``` clojure
(let [mentor   (create-mentor! {:name "Marc" :surname "Lauryn"})
      band     (create-band! {:name     "The Unfos" :genre "RnR" :largeness 5
                              :category nil})
      musician (create-musician! {:name       "Philip" :surname "Yonas"
                                  :nickname   "Fisha"  :band    nil
                                  :dream_band nil})]
  (assoc-musician-band! {:band (:id band) :musician (:id musician)})
  (assoc-musician-mentor! {:musician (:id musician) :mentor (:id mentor)})
  (println (str (:name musician) " is rockin' with "
                (-> {:musician (:id musician)}
                    get-musician-band
                    :name)
                " thanks to "
                (-> {:musician (:id musician)}
                    get-musician-mentor
                    :name) "!")))

=> Philip is rockin' with The Unfos thanks to Marc!
```

## License

Copyright © 2015-2016 Novak Boškov

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

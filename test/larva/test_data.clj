(ns larva.test-data
  "Test cases used in testing Larva")

(def no-entities-edge-case
  {:about
   {:name    "Pilot model"
    :author  "Novak Boskov"
    :comment "This is just in the sake of a proof of concept."}
   :entities
   []})

(def no-entities-no-about-edge-case
  {:entities
   []})

(def standard-program-1
  {:about
   {:name    "Pilot model"
    :author  "Novak Boskov"
    :comment "This is just in the sake of a proof of concept."}
   :entities
   [{:signature  "Musician"
     :properties [{:name "name" :type :str :gui-label "Name"}
                  {:name "surname" :type :str :gui-label "Surname"}
                  {:name "nickname" :type :str :gui-label "nick"}
                  {:name "honors" :type {:coll :str}}]}
    {:signature  "Band"
     :properties [{:name "name" :type :str :gui-label "Name"}
                  {:name "genre" :type :str :gui-label "Genre"}
                  {:name "largeness" :type :str :gui-label "Largeness"}
                  {:name "members" :type {:coll :ref-to :signature "Musician"} :gui-label "Members"}]}
    {:signature  "Festival"
     :properties [{:name "name" :type :str :gui-label "Name"}
                  {:name "location" :type :geo :gui-label "Loco"}]}]})

(def entity-have-no-properties
  {:about
   {:name    "Pilot model"
    :author  "Novak Boskov"
    :comment "This is just in the sake of a proof of concept."}
   :entities
   [{:signature  "Musician"
     :properties [{:name "name" :type :str :gui-label "Name"}
                  {:name "surname" :type :str :gui-label "Surname"}
                  {:name "nickname" :type :str :gui-label "nick"}
                  {:name "honors" :type {:coll :str}}]}
    {:signature  "Band"
     :properties []}
    {:signature  "Festival"
     :properties [{:name "name" :type :str :gui-label "Name"}
                  {:name "location" :type :geo :gui-label "Loco"}]}]})

(def standard-program-no-refs
  {:about
   {:name    "Pilot model"
    :author  "Novak Boskov"
    :comment "This is just in the sake of a proof of concept."}
   :entities
   [{:signature  "Musician"
     :properties [{:name "name" :type :str :gui-label "Name"}
                  {:name "surname" :type :str :gui-label "Surname"}
                  {:name "nickname" :type :str :gui-label "nick"}
                  {:name "honors" :type {:coll :str}}]}
    {:signature  "Band"
     :properties [{:name "name" :type :str :gui-label "Name"}
                  {:name "genre" :type :str :gui-label "Genre"}
                  {:name "largeness" :type :str :gui-label "Largeness"}]}
    {:signature  "Festival"
     :properties [{:name "name" :type :str :gui-label "Name"}
                  {:name "location" :type :geo :gui-label "Loco"}]}]})

(def standard-program-11
  {:about
   {:name    "Pilot model"
    :author  "Novak Boskov"
    :comment "This is just in the sake of a proof of concept."}
   :entities
   [{:signature  "Musician"
     :properties [{:name "name" :type :str :gui-label "Name"}
                  {:name "surename" :type :str :gui-label "Surname"}
                  {:name "nickname" :type :str :gui-label "nick"}
                  {:name "honors" :type {:coll :str}}]}
    {:signature  "Band"
     :properties [{:name "name" :type :str :gui-label "Name"}
                  {:name "genre" :type :str :gui-label "Genre"}
                  {:name "largeness" :type :str :gui-label "Largeness"}
                  {:name "members" :type {:coll :ref-to :signature "Musician"} :gui-label "Members"}]}
    {:signature  "Fan"
     :properties [{:name "name" :type :str :gui-label "Name"}
                  {:name "surname" :type :str :gui-label "Surname"}
                  {:name "gender" :type :str :gui-label "Gender"}
                  {:name "bands" :type {:coll :ref-to :signature "Band"} :gui-label "Beloved bands"}]}
    {:signature  "Festival"
     :properties [{:name "name" :type :str :gui-label "Name"}
                  {:name "location" :type :geo :gui-label "Loco"}]}]})

(def standard-program-2
  {:entities
   [{:signature  "Musician"
     :properties [{:name "name" :type :str :gui-label "Name"}
                  {:name "surename" :type :str :gui-label "Surname"}
                  {:name "nickname" :type :str :gui-label "nick"}
                  {:name "honors" :type {:coll :str}}]}
    {:signature  "Band"
     :properties [{:name "name" :type :str :gui-label "Name"}
                  {:name "genre" :type :str :gui-label "Genre"}
                  {:name "largeness" :type :str :gui-label "Largeness"}
                  {:name "members" :type {:coll :ref-to :signature "Musician"} :gui-label "Members"}]}
    {:signature  "Festival"
     :properties [{:name "name" :type :str :gui-label "Name"}
                  {:name "location" :type :geo :gui-label "Loco"}]}]})

(def standard-program-cardinality-1
  "Test case contains one to many, one to one cardinality and whole-part
  relationship."
  {:entities
   [{:signature  "Musician"
     :properties [{:name "name" :type :str :gui-label "Name"}
                  {:name "surname" :type :str :gui-label "Surname"}
                  {:name "nickname" :type :str :gui-label "nick"}
                  {:name "honors" :type {:coll :str}}]}
    {:signature  "Band"
     :properties [{:name "name" :type :str :gui-label "Name"}
                  {:name "genre" :type :str :gui-label "Genre"}
                  {:name "largeness" :type :str :gui-label "Largeness"}
                  {:name "members" :type {:coll :ref-to :signature "Musician"} :gui-label "Members"}
                  {:name "category" :type {:one :ref-to :signature "Category"} :gui-label "Category"}]}
    {:signature  "Category"
     :properties [{:name "name" :type :str :gui-label "Name"}
                  {:name      "subcategories" :type {:coll :ref-to :signature "Category"}
                   :gui-label "subcategories"}]}
    {:signature  "Festival"
     :properties [{:name "name" :type :str :gui-label "Name"}
                  {:name "location" :type :geo :gui-label "Loco"}]}]})

(def references-1
  "Test case with one->many, many->one, many->many, one->one relationships."
  {:about
   {:name    "Pilot model"
    :author  "Novak Boskov"
    :comment "This is just in the sake of a proof of concept."}
   :entities
   [{:signature  "Musician"
     :properties [{:name "name" :type :str :gui-label "Name"}
                  {:name "surname" :type :str :gui-label "Surname"}
                  {:name "nickname" :type :str :gui-label "nick"}
                  {:name "honors" :type {:coll :str}}
                  {:name      "band" :type {:one :ref-to :signature "Band"}
                   :gui-label "Of band"}
                  {:name      "social-profile" :type {:one :ref-to :signature
                                                      "SocialMediaProfile"}
                   :gui-label "profile"}]}
    {:signature  "Band"
     :properties [{:name "name" :type :str :gui-label "Name"}
                  {:name "genre" :type :str :gui-label "Genre"}
                  {:name "largeness" :type :str :gui-label "Largeness"}
                  {:name      "members" :type {:coll :ref-to :signature "Musician"}
                   :gui-label "Members"}
                  {:name      "category" :type {:one :ref-to :signature "Category"}
                   :gui-label "Category"}
                  {:name      "participated" :type {:coll :ref-to :signature "Festival"}
                   :gui-label "Participated in"}]}
    {:signature  "Category"
     :properties [{:name "name" :type :str :gui-label "Name"}
                  {:name      "subcategories" :type {:coll :ref-to :signature "Category"}
                   :gui-label "subcategories"}
                  {:name      "bands" :type {:coll :ref-to :signature "Band"}
                   :gui-label "Bands of category"}]}
    {:signature  "Festival"
     :properties [{:name "name" :type :str :gui-label "Name"}
                  {:name "location" :type :geo :gui-label "Loco"}
                  {:name      "bands" :type {:coll :ref-to :signature "Band"}
                   :gui-label "participant bands"}]}
    {:signature  "SocialMediaProfile"
     :properties [{:name      "owner" :type {:one :ref-to :signature "Musician"}
                   :gui-label "Name"}
                  {:name "name" :type :str :gui-label "name"}
                  {:name "provider" :type :str :gui-label "provider"}]}]})

(def standard-program-with-meta
  {:about
   {:name    "Pilot model"
    :author  "Novak Boskov"
    :comment "This is just in the sake of a proof of concept."}
   :meta
   {:api-only false
    :db       :postgres}
   :entities
   [{:signature  "Musician"
     :properties [{:name "name" :type :str :gui-label "Name"}
                  {:name "surname" :type :str :gui-label "Surname"}
                  {:name "nickname" :type :str :gui-label "nick"}
                  {:name "honors" :type {:coll :str}}
                  {:name      "band" :type {:one       :ref-to
                                            :signature "Band"
                                            :gui       :select-form}
                   :gui-label "Of band"}
                  {:name      "social-profile" :type {:one       :ref-to
                                                      :signature "SocialMediaProfile"}
                   :gui-label "profile"}]}
    {:signature  "Band"
     :properties [{:name "name" :type :str :gui-label "Name"}
                  {:name "genre" :type :str :gui-label "Genre"}
                  {:name "largeness" :type :str :gui-label "Largeness"}
                  {:name      "members" :type {:coll :ref-to :signature "Musician"}
                   :gui-label "Members"}
                  {:name      "category" :type {:one       :ref-to
                                                :signature "Category"
                                                :gui       :drop-list}
                   :gui-label "Category"}
                  {:name      "participated" :type {:coll      :ref-to
                                                    :signature "Festival"
                                                    :gui       :table-view}
                   :gui-label "Participated in"}]}
    {:signature  "Category"
     :properties [{:name "name" :type :str :gui-label "Name"}
                  {:name      "subcategories" :type {:coll      :ref-to
                                                     :signature "Category"
                                                     :gui       :table-view}
                   :gui-label "subcategories"}
                  {:name      "bands" :type {:coll      :ref-to
                                             :signature "Band"
                                             :gui       :table-view}
                   :gui-label "Bands of category"}]}
    {:signature  "Festival"
     :properties [{:name "name" :type :str :gui-label "Name"}
                  {:name "location" :type :geo :gui-label "Loco"}
                  {:name      "bands" :type {:coll      :ref-to
                                             :signature "Band"
                                             :gui       :table-view}
                   :gui-label "participant bands"}]}
    {:signature  "SocialMediaProfile"
     :properties [{:name      "owner" :type {:one       :ref-to
                                             :signature "Musician"
                                             :gui       :select-form}
                   :gui-label "Name"}
                  {:name "name" :type :str :gui-label "name"}
                  {:name "provider" :type :str :gui-label "provider"}]}]})

(def entities-with-signature-plural
  {:about
   {:name    "Pilot model"
    :author  "Novak Boskov"
    :comment "This is just in the sake of a proof of concept."}
   :meta
   {:api-only false}
   :entities
   [{:signature  "Musician"
     :properties [{:name "name" :type :str :gui-label "Name"}
                  {:name "surname" :type :str :gui-label "Surname"}
                  {:name "nickname" :type :str :gui-label "nick"}
                  {:name "honors" :type {:coll :str}}
                  {:name      "band" :type {:one       :ref-to
                                            :signature "Band"
                                            :gui       :select-form}
                   :gui-label "Of band"}
                  {:name      "social-profile" :type {:one       :ref-to
                                                      :signature "SocialMediaProfile"}
                   :gui-label "profile"}]}
    {:signature  "Band"
     :plural     "Bands"
     :properties [{:name "name" :type :str :gui-label "Name"}
                  {:name "genre" :type :str :gui-label "Genre"}
                  {:name "largeness" :type :str :gui-label "Largeness"}
                  {:name      "members" :type {:coll :ref-to :signature "Musician"}
                   :gui-label "Members"}
                  {:name      "category" :type {:one       :ref-to
                                                :signature "Category"
                                                :gui       :drop-list}
                   :gui-label "Category"}
                  {:name      "participated" :type {:coll      :ref-to
                                                    :signature "Festival"
                                                    :gui       :table-view}
                   :gui-label "Participated in"}]}
    {:signature  "Category"
     :plural     "Categories"
     :properties [{:name "name" :type :str :gui-label "Name"}
                  {:name      "subcategories" :type {:coll      :ref-to
                                                     :signature "Category"
                                                     :gui       :table-view}
                   :gui-label "subcategories"}
                  {:name      "bands" :type {:coll      :ref-to
                                             :signature "Band"
                                             :gui       :table-view}
                   :gui-label "Bands of category"}]}
    {:signature  "Festival"
     :properties [{:name "name" :type :str :gui-label "Name"}
                  {:name "location" :type :geo :gui-label "Loco"}
                  {:name      "bands" :type {:coll      :ref-to
                                             :signature "Band"
                                             :gui       :table-view}
                   :gui-label "participant bands"}]}
    {:signature  "SocialMediaProfile"
     :properties [{:name      "owner" :type {:one       :ref-to
                                             :signature "Musician"
                                             :gui       :select-form}
                   :gui-label "Name"}
                  {:name "name" :type :str :gui-label "name"}
                  {:name "provider" :type :str :gui-label "provider"}]}]})

(def no-entities-no-about-empty-meta
  {:meta {}
   :entities
   []})

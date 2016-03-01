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
                  {:name "honors" :type {:coll :str}}
                  {:name "band" :type {:one :reference
                                       :to  ["Band"]}}]}
    {:signature  "Band"
     :properties [{:name "name" :type :str :gui-label "Name"}
                  {:name "genre" :type :str :gui-label "Genre"}
                  {:name "largeness" :type :str :gui-label "Largeness"}
                  {:name      "members" :type {:coll :reference
                                               :to   ["Musician" "band"]}
                   :gui-label "Members"}]}
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
                  {:name "honors" :type {:coll :str}}
                  {:name "band" :type {:one :reference
                                       :to  ["Band" "members"]}}]}
    {:signature  "Band"
     :properties [{:name "name" :type :str :gui-label "Name"}
                  {:name "genre" :type :str :gui-label "Genre"}
                  {:name "largeness" :type :str :gui-label "Largeness"}
                  {:name      "members" :type {:coll :reference
                                               :to   ["Musician"]}
                   :gui-label "Members"}
                  {:name "fans" :type {:coll :reference
                                       :to   ["Fan" "bands"]}}]}
    {:signature  "Fan"
     :properties [{:name "name" :type :str :gui-label "Name"}
                  {:name "surname" :type :str :gui-label "Surname"}
                  {:name "gender" :type :str :gui-label "Gender"}
                  {:name      "bands" :type {:coll :reference
                                             :to   ["Band"]}
                   :gui-label "Beloved bands"}]}
    {:signature  "Festival"
     :properties [{:name "name" :type :str :gui-label "Name"}
                  {:name "location" :type :geo :gui-label "Loco"}]}]})

(def standard-program-2
  {:entities
   [{:signature  "Musician"
     :properties [{:name "name" :type :str :gui-label "Name"}
                  {:name "surename" :type :str :gui-label "Surname"}
                  {:name "nickname" :type :str :gui-label "nick"}
                  {:name "honors" :type {:coll :str}}
                  {:name "band" :type {:one :reference
                                       :to  ["Band" "members"]}}]}
    {:signature  "Band"
     :properties [{:name "name" :type :str :gui-label "Name"}
                  {:name "genre" :type :str :gui-label "Genre"}
                  {:name "largeness" :type :str :gui-label "Largeness"}
                  {:name      "members" :type {:coll :reference
                                               :to   ["Musician"]}
                   :gui-label "Members"}]}
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
                  {:name "honors" :type {:coll :str}}
                  {:name "band" :type {:one :reference
                                       :to  ["Band" "members"]}}]}
    {:signature  "Band"
     :properties [{:name "name" :type :str :gui-label "Name"}
                  {:name "genre" :type :str :gui-label "Genre"}
                  {:name "largeness" :type :str :gui-label "Largeness"}
                  {:name      "members" :type {:coll :reference
                                               :to   ["Musician"]}
                   :gui-label "Members"}
                  {:name      "category" :type {:one :reference
                                                :to  ["Category" "bands"]}
                   :gui-label "Category"}]}
    {:signature  "Category"
     :properties [{:name "name" :type :str :gui-label "Name"}
                  {:name      "subcategories" :type
                   {:coll :reference
                    :to   ["Category" "subcategories"]}
                   :gui-label "subcategories"}
                  {:name "bands" :type {:coll :reference
                                        :to   ["Band"]}}]}
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
                  {:name      "band" :type {:one :reference
                                            :to  ["Band"]}
                   :gui-label "Of band"}
                  {:name      "social-profile" :type
                   {:one :reference
                    :to  ["SocialMediaProfile"]}
                   :gui-label "profile"}]}
    {:signature  "Band"
     :properties [{:name "name" :type :str :gui-label "Name"}
                  {:name "genre" :type :str :gui-label "Genre"}
                  {:name "largeness" :type :str :gui-label "Largeness"}
                  {:name      "members" :type {:coll :reference
                                               :to   ["Musician" "band"]}
                   :gui-label "Members"}
                  {:name      "category" :type {:one :reference
                                                :to  ["Category" "bands"]}
                   :gui-label "Category"}
                  {:name      "participated" :type
                   {:coll :reference
                    :to   ["Festival" "participants"]}
                   :gui-label "Participated in"}]}
    {:signature  "Category"
     :properties [{:name "name" :type :str :gui-label "Name"}
                  {:name      "subcategories" :type
                   {:coll :reference :to ["Category" "subcategories"]}
                   :gui-label "subcategories"}
                  {:name      "bands" :type {:coll :reference :to ["Band"]}
                   :gui-label "Bands of category"}]}
    {:signature  "Festival"
     :properties [{:name "name" :type :str :gui-label "Name"}
                  {:name "location" :type :geo :gui-label "Loco"}
                  {:name      "participants" :type {:coll :reference
                                                    :to   ["Band"]}
                   :gui-label "participant bands"}]}
    {:signature  "SocialMediaProfile"
     :properties [{:name      "owner" :type {:one :reference
                                             :to  ["Musician" "social-profile"]}
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
    :db       {:type :postgres :sql :yesql}}
   :entities
   [{:signature  "Musician"
     :properties [{:name "name" :type :str :gui-label "Name"}
                  {:name "surname" :type :str :gui-label "Surname"}
                  {:name "nickname" :type :str :gui-label "nick"}
                  {:name "honors" :type {:coll :str}}
                  {:name      "band" :type {:one :reference
                                            :to  ["Band"]
                                            :gui :select-form}
                   :gui-label "Of band"}
                  {:name      "social-profile" :type
                   {:one :reference
                    :to  ["SocialMediaProfile"]}
                   :gui-label "profile"}]}
    {:signature  "Band"
     :properties [{:name "name" :type :str :gui-label "Name"}
                  {:name "genre" :type :str :gui-label "Genre"}
                  {:name "largeness" :type :str :gui-label "Largeness"}
                  {:name      "members" :type {:coll :reference
                                               :to   ["Musician" "band"]}
                   :gui-label "Members"}
                  {:name      "category" :type {:one :reference
                                                :to  ["Category" "bands"]
                                                :gui :select-form}
                   :gui-label "Category"}
                  {:name      "participated" :type
                   {:coll :reference
                    :to   ["Festival" "participants"]}
                   :gui-label "Participated in"}]}
    {:signature  "Category"
     :properties [{:name "name" :type :str :gui-label "Name"}
                  {:name      "subcategories" :type
                   {:coll :reference :to ["Category" "subcategories"]
                    :gui  :table-view}
                   :gui-label "subcategories"}
                  {:name      "bands" :type {:coll :reference :to ["Band"]
                                             :gui  :table-view}
                   :gui-label "Bands of category"}]}
    {:signature  "Festival"
     :properties [{:name "name" :type :str :gui-label "Name"}
                  {:name "location" :type :geo :gui-label "Loco"}
                  {:name      "participants" :type {:coll :reference
                                                    :to   ["Band"]
                                                    :gui  :table-view}
                   :gui-label "participant bands"}]}
    {:signature  "SocialMediaProfile"
     :properties [{:name      "owner" :type {:one :reference
                                             :to  ["Musician" "social-profile" ]
                                             :gui :select-form}
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
                  {:name      "band" :type {:one :reference
                                            :to  ["Band"]
                                            :gui :select-form}
                   :gui-label "Of band"}
                  {:name      "social-profile" :type
                   {:one :reference
                    :to  ["SocialMediaProfile"]}
                   :gui-label "profile"}]}
    {:signature  "Band"
     :plural     "Bands"
     :properties [{:name "name" :type :str :gui-label "Name"}
                  {:name "genre" :type :str :gui-label "Genre"}
                  {:name "largeness" :type :str :gui-label "Largeness"}
                  {:name      "members" :type {:coll :reference
                                               :to   ["Musician" "band"]}
                   :gui-label "Members"}
                  {:name      "category" :type {:one :reference
                                                :to  ["Category" "bands"]
                                                :gui :select-form}
                   :gui-label "Category"}
                  {:name      "participated" :type
                   {:coll :reference
                    :to   ["Festival" "participants"]}
                   :gui-label "Participated in"}]}
    {:signature  "Category"
     :plural     "Categories"
     :properties [{:name "name" :type :str :gui-label "Name"}
                  {:name      "subcategories" :type
                   {:coll :reference :to ["Category" "subcategories"]
                    :gui  :table-view}
                   :gui-label "subcategories"}
                  {:name      "bands" :type {:coll :reference :to ["Band"]
                                             :gui  :table-view}
                   :gui-label "Bands of category"}]}
    {:signature  "Festival"
     :properties [{:name "name" :type :str :gui-label "Name"}
                  {:name "location" :type :geo :gui-label "Loco"}
                  {:name      "participants" :type {:coll :reference
                                                    :to   ["Band"]
                                                    :gui  :table-view}
                   :gui-label "participant bands"}]}
    {:signature  "SocialMediaProfile"
     :properties [{:name      "owner" :type {:one :reference
                                             :to  ["Musician" "social-profile"]
                                             :gui :select-form}
                   :gui-label "Name"}
                  {:name "name" :type :str :gui-label "name"}
                  {:name "provider" :type :str :gui-label "provider"}]}]})

(def no-entities-no-about-empty-meta
  {:meta {}
   :entities
   []})

(def custom-property-datatype
  {:about
   {:name    "Pilot"
    :author  "Novak Boskov"
    :comment "This is just in the sake of a proof of concept."}
   :meta
   {:api-only false
    :db       {:type :postgres :sql :hugsql}}
   :entities
   [{:signature  "Musician"
     :properties [{:name "name" :type :str :gui-label "Name"}
                  {:name "surname" :type :str :gui-label "Surname"}
                  {:name "nickname" :type "VARCHAR(20)" :gui-label "nick"}
                  {:name "honors" :type {:coll :str}}
                  {:name      "band" :type {:one :reference
                                            :to  ["Band" "members"]
                                            :gui :select-form}
                   :gui-label "Of band"}
                  {:name      "dream band" :type {:one :reference
                                                  :to  ["Band" "dream about"]
                                                  :gui :select-form}
                   :gui-label "Dream about"}
                  {:name      "social-profile" :type
                   {:one :reference
                    :to  ["SocialMediaProfile" "owner"]}
                   :gui-label "profile"}
                  {:name "instruments" :type {:coll :reference
                                              :to   ["Instrument" "players"]
                                              :gui  :table-view}}
                  {:name "knows how to repair" :type
                   {:coll :reference
                    :to   ["Instrument" "repairers"]
                    :gui  :table-view}}
                  {:name "guru" :type {:one :reference
                                       :to  ["Musician" "guru"]
                                       :gui :select-form}}
                  {:name "disrespected by" :type {:one :reference
                                                  :to  ["Mentor" "disrespect"]
                                                  :gui :select-form}}
                  {:name "mentor" :type {:one :reference
                                         :to  ["Mentor" "learner"]
                                         :gui :select-form}}]}
    {:signature  "Band"
     :plural     "Bands"
     :properties [{:name "name" :type :str :gui-label "Name"}
                  {:name "genre" :type :str :gui-label "Genre"}
                  {:name "largeness" :type :num :gui-label "Largeness"}
                  {:name      "members" :type {:coll :reference
                                               :to   ["Musician"]}
                   :gui-label "Members"}
                  {:name      "dream about" :type {:coll :reference
                                                   :to   ["Musician"]}
                   :gui-label "Dream about"}
                  {:name      "category" :type {:one :reference
                                                :to  ["Category" "bands"]
                                                :gui :drop-list}
                   :gui-label "Category"}
                  {:name      "participated" :type {:coll :reference
                                                    :to   ["Festival"]
                                                    :gui  :table-view}
                   :gui-label "Participated in"}
                  {:name "influenced" :type {:coll :reference
                                             :to   ["Band" "influenced"]
                                             :gui  :table-view}}]}
    {:signature  "Category"
     :plural     "Categories"
     :properties [{:name "name" :type :str :gui-label "Name"}
                  {:name      "subcategories" :type
                   {:coll :reference
                    :to   ["Category" "subcategories"]
                    :gui  :table-view}
                   :gui-label "subcategories"}
                  {:name      "bands" :type {:coll :reference
                                             :to   ["Band"]
                                             :gui  :table-view}
                   :gui-label "Bands of category"}]}
    {:signature  "Festival"
     :properties [{:name "name" :type :str :gui-label "Name"}
                  {:name "location" :type "POINT" :gui-label "Loco"}
                  {:name      "participants" :type {:coll :reference
                                                    :to   ["Band" "participated"]
                                                    :gui  :table-view}
                   :gui-label "participant bands"}]}
    {:signature  "SocialMediaProfile"
     :properties [{:name      "owner" :type {:one :reference
                                             :to  ["Musician"]
                                             :gui :select-form}
                   :gui-label "Name"}
                  {:name "name" :type :str :gui-label "name"}
                  {:name "provider" :type :str :gui-label "provider"}]}
    {:signature  "Mentor"
     :properties [{:name "name" :type :str :gui-label "Name"}
                  {:name "surname" :type :str :gui-label "Surname"}
                  {:name "learner" :type {:one :reference
                                          :to  ["Musician"]
                                          :gui :select-form}}
                  {:name "disrespect" :type {:one :reference
                                             :to  ["Musician"]
                                             :gui :select-form}}]
     :plural     "Mentors"}
    {:signature  "more info"
     :properties [{:name "more info" :type :str}]}
    {:signature  "Instrument"
     :properties [{:name "name" :type :str :gui-label "Name"}
                  {:name "players" :type {:coll :reference
                                          :to   ["Musician"]
                                          :gui  :table-view}}
                  {:name "repairers" :type {:coll :reference
                                            :to   ["Musician"]
                                            :gui  :table-view}}
                  {:name "type" :type :str :gui-label "Instrument type"}]}]})

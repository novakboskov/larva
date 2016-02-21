{:about
   {:name    "Pilot"
    :author  "Novak Boskov"
    :comment "This is just in the sake of a proof of concept."}
   :meta
   {:api-only false
    :db       :postgres}
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
                   :gui-label "Members"}
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
                  {:name "type" :type :str :gui-label "Instrument type"}]}]}

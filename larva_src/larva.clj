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
                {:name "salute" :type :str}
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
                {:name "provider" :type :str :gui-label "provider"}]}]}

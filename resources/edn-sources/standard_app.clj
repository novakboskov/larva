{:about
 {:name "Pilot model"
  :author "Novak Boskov"
  :comment "This is just in the sake of a proof of concept."}
 :entities
 [{:signature "Musician"
   :properties [{:name "name" :type :str :gui-label "Name"}
                {:name "surname" :type :str :gui-label "Surname"}
                {:name "nickname" :type :str :gui-label "nick"}
                {:name "honors" :type {:coll :str}}
                {:name "band" :type {:one :ref-to :signature "Band"}
                 :gui-label "Of band"}
                {:name "social-profile" :type {:one :ref-to :signature
                                               "SocialMediaProfile"}
                 :gui-label "profile"}]}
  {:signature "Band"
   :properties [{:name "name" :type :str :gui-label "Name"}
                {:name "genre" :type :str :gui-label "Genre"}
                {:name "largeness" :type :str :gui-label "Largeness"}
                {:name "members" :type {:coll :ref-to :signature "Musician"}
                 :gui-label "Members"}
                {:name "category" :type {:one :ref-to :signature "Category"}
                 :gui-label "Category"}
                {:name "participated" :type {:coll :ref-to :signature "Festival"}
                 :gui-label "Participated in"}]}
  {:signature "Category"
   :properties [{:name "name" :type :str :gui-label "Name"}
                {:name "subcategories" :type {:coll :ref-to :signature "Category"}
                 :gui-label "subcategories"}
                {:name "bands" :type {:coll :ref-to :signature "Band"}
                 :gui-label "Bands of category"}]}
  {:signature "Festival"
   :properties [{:name "name" :type :str :gui-label "Name"}
                {:name "location" :type :geo :gui-label "Loco"}
                {:name "bands" :type {:coll :ref-to :signature "Band"}
                 :gui-label "participant bands"}]}
  {:signature "SocialMediaProfile"
   :properties [{:name "owner" :type {:one :ref-to :signature "Musician"}
                 :gui-label "Name"}
                {:name "name" :type :str :gui-label "name"}
                {:name "provider" :type :str :gui-label "provider"}]}]}

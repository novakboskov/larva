{:about
 {:name "Pilot model"
  :author "Novak Boskov"
  :comment "This is just in the sake of a proof of concept."}
 :entities
 [{:signature "Musician"
   :properties [{:name "name" :type :str :gui-label "Name"}
                {:name "surname" :type :str :gui-label "Surname"}
                {:name "nickname" :type :str :gui-label "nick"}
                {:name "honors" :type {:coll :str}}]}
  {:signature "Band"
   :properties [{:name "name" :type :str :gui-label "Name"}
                {:name "genre" :type :str :gui-label "Genre"}
                {:name "largeness" :type :str :gui-label "Largeness"}
                {:name "members" :type {:coll :ref-to :signature "Musician"} :gui-label "Members"}]}
  {:signature "Festival"
   :properties [{:name "name" :type :str :gui-label "Name"}
                {:name "location" :type :geo :gui-label "Loco"}]}]}

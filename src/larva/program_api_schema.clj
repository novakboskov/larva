(ns larva.program-api-schema
  (:require [schema.core :as s]))

(s/def APIEntities
  [s/Str])

(s/def APISimpleDataType
  (s/enum :str :num :geo :datetime))

(s/def APICollection
  "Simple collection property."
  {:coll APISimpleDataType})

(s/def APICollectionWithReference
  {:coll (s/enum :ref-to)
   :signature s/Str
   :gui (s/maybe (s/enum :table-view))})

(s/def APIReferenceToSingleEntity
  {:one (s/enum :ref-to)
   :signature s/Str
   :gui (s/maybe (s/enum :select-form :drop-list))})

(s/def APISomethingWithReference
  "Something which refers to collection of entities or single entity determined by :signature."
  (s/conditional
   #(contains? % :coll) APICollectionWithReference
   :else APIReferenceToSingleEntity))

(s/def APIPropertyDataType
  (s/conditional
   #(keyword? %) APISimpleDataType
   #(= 1 (count %)) APICollection
   :else APISomethingWithReference))

(s/def APIProperty
  {:name s/Str :type APIPropertyDataType
   :gui-label (s/maybe s/Str)})

(s/def APIProperties
  [APIProperty])

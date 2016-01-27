(ns larva.api-schemes
  (:require [schema.core :as s]))

(s/def Entities
  [s/Str])

(s/def SimpleDataType
  (s/enum :str :num :geo :datetime))

(s/def Collection
  "Simple collection property."
  {:coll SimpleDataType})

(s/def CollectionWithReference
  {:coll (s/enum :ref-to)
   :signature s/Str
   :gui (s/enum :table-view)})

(s/def ReferenceToSingleEntity
  {:one (s/enum :ref-to)
   :signature s/Str
   :gui (s/enum :select-form :drop-list)})

(s/def SomethingWithReference
  "Something which refers to collection of entities or single entity determined by :signature."
  (s/conditional
   #(contains? % :coll) CollectionWithReference
   :else ReferenceToSingleEntity))

(s/def PropertyDataType
  (s/conditional
   #(keyword? %) SimpleDataType
   #(= 1 (count %)) Collection
   :else SomethingWithReference))

(s/def Property
  {:name s/Str :type PropertyDataType
   :gui-label (s/maybe s/Str)})

(s/def Properties
  [Property])

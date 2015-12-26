(ns larva.meta-model
  (:require [schema.core :as s]))

;;;;;;
;; Defining "meta-model" in the form of system of schemes.
;;;;;;

(s/def SimpleDataType
  (s/enum :str :num :geo :datetime))

(s/def Collection
  "Simple collection property."
  {:coll SimpleDataType})

(s/def CollectionWithReference
  {:coll (s/enum :ref-to) :signature s/Str})

(s/def ReferenceToSingleEntity
  {:one (s/enum :ref-to) :signature s/Str})

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
  {:name s/Str (s/optional-key :type) PropertyDataType
   (s/optional-key :gui-label) s/Str})

(s/def Properties
  [Property])

(s/def Entity
  {:signature s/Str
   :properties Properties})

(s/def Entities
  [Entity])

(s/def About
  "Schema for about section of program."
  {:name s/Str (s/optional-key :author) s/Str
   (s/optional-key :comment) s/Str})

(s/def Program
  "Schema for standard app program."
  {(s/optional-key :about) About
   :entities Entities})

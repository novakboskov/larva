(ns larva.program-api-schemes
  "Schemes used in program API. This schemes don't need to be the same as those in metamodel.
  Some of this schemes may be used for representing data in the form which is more
  suitable to various code generators."
  (:require [schema.core :as s]
            [larva.meta-model :as mm]))

(s/def APIEntities
  [s/Str])

(s/def APIEntityInfo
  {:signature s/Str (s/optional-key :plural) s/Str})

(def APIDataType mm/DataType)

(def APISimpleDataType mm/SimpleDataType)

(def APICustomDataType mm/CustomDataType)

(def APICollection mm/Collection)

(def APICollectionWithReference mm/CollectionWithReference)

(def APIReferenceToSingleEntity mm/ReferenceToSingleEntity)

(def APISomethingWithReference mm/SomethingWithReference)

(def APIPropertyDataType mm/PropertyDataType)

(def APIProperty mm/Property)

(def APIProperties mm/Properties)

(def APIMeta mm/Meta)

(def APIAbout mm/About)

(def APIProgram mm/Program)

(s/def APIPropertyReference
  (s/conditional
   #(keyword? %) (s/enum :not-a-reference)
   #(map? %) {(s/enum :one-to-many
                      :many-to-one
                      :one-to-one
                      :many-to-many)              s/Str
              (s/optional-key :recursive)         s/Bool
              (s/optional-key :back-property)     s/Str
              (s/optional-key :simple-collection) (s/enum :pseudo-reference)}))

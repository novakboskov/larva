(ns larva.program-api-schemes
  "Schemes used in program API. This schemes don't need to be the same as those in metamodel.
  Some of this schemes may be used for representing data in the form which is more
  suitable to various code generators."
  (:require [schema.core :as s]
            [larva.meta-model :as mm]))

(s/def APIEntities
  [s/Str])

(def APISimpleDataType mm/SimpleDataType)

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

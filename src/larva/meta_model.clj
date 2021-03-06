(ns larva.meta-model
  "Defining meta-model in the form of system of schemes."
  (:require [schema.core :as s]))

(s/def SimpleDataType
  (s/enum :str :text
          :num :float :bignum
          :datetime :date :timestamp
          :bool :geo :json :binary :pass))

(s/def CustomDataType s/Str)

(s/def DataType
  (s/cond-pre SimpleDataType CustomDataType))

(s/def Collection
  "Simple collection property."
  {:coll DataType})

(s/def SignatureProperty
  (s/conditional
   #(or (= (count %) 2) (= (count %) 1))
   [(s/one s/Str "signature") (s/optional s/Str "property")]))

(s/def CollectionWithReference
  {:coll                 (s/enum :reference)
   :to                   SignatureProperty
   ;; optional GUI representation of referenced collection
   (s/optional-key :gui) (s/enum :table-view)})

(s/def ReferenceToSingleEntity
  {:one                  (s/enum :reference)
   :to                   SignatureProperty
   ;; optional GUI representation of referenced think
   (s/optional-key :gui) (s/enum :select-form :drop-list)})

(s/def SomethingWithReference
  "Something which refers to collection of entities or single entity determined by :signature."
  (s/conditional
   #(contains? % :coll) CollectionWithReference
   :else ReferenceToSingleEntity))

(s/def PropertyDataType
  (s/conditional
   #(keyword? %) SimpleDataType
   #(string? %) CustomDataType
   #(and (map? %) (= 1 (count %))) Collection
   :else SomethingWithReference))

(s/def Property
  {:name                       s/Str
   (s/optional-key :type)      PropertyDataType
   (s/optional-key :gui-label) s/Str})

(s/def Properties
  [Property])

(s/def Entity
  {:signature               s/Str
   :properties              Properties
   (s/optional-key :plural) s/Str})

(s/def Entities
  [Entity])

(s/def DBTypes
  (s/enum :postgres :mysql :h2 :sqlite :mongodb))

(s/def SQLTools
  (s/enum :hugsql :yesql))

(s/def Meta
  "Schema for meta section of program."
  {(s/optional-key :api-only) s/Bool
   (s/optional-key :db)       {:type DBTypes (s/optional-key :sql) SQLTools}})

(s/def About
  "Schema for about section of program."
  {:name                     s/Str
   (s/optional-key :author)  s/Str
   (s/optional-key :comment) s/Str})

(s/def Program
  "Schema for standard app program."
  {(s/optional-key :about) About
   (s/optional-key :meta)  Meta
   :entities               Entities})

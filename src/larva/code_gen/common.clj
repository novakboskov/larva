(ns larva.code-gen.common
  "Code taken from luminus-template.
  See: https://github.com/luminus-framework/luminus-template"
  (:require [leiningen.new.templates :refer [renderer ->files]]
            [selmer.parser :as selmer]))

(defn render-template [template options]
  (selmer/render
   (str "<% safe %>" template "<% endsafe %>")
   options
   {:tag-open \< :tag-close \> :filter-open \< :filter-close \>}))

(defonce larva-render (renderer "larva" render-template))

(defn render-asset [render options asset]
  (if (string? asset)
    asset
    (let [[target source] asset]
      [target (larva-render source options)])))

(defn render-assets [assets options]
  (apply ->files options (map #(render-asset options %) assets)))

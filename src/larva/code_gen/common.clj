(ns larva.code-gen.common
  "Code taken from luminus-template.
  See: https://github.com/luminus-framework/luminus-template"
  (:require [clojure.java.io :as io]
            [larva.my.leiningen.new.templates :as mylnt]
            [leiningen.new.templates :as lnt]
            [selmer.parser :as selmer]))

(defn render-template [template options]
  (selmer/render
   (str "<% safe %>" template "<% endsafe %>")
   options
   {:tag-open \< :tag-close \> :filter-open \< :filter-close \>}))

(defonce larva-render (mylnt/renderer "larva" render-template "templates"))

(defn render-asset [render options asset]
  ^{:break/when (try (let [[a b] asset] [a b])
                     (catch Exception e true))}
  (if (string? asset)
    asset
    (let [[target source] asset]
      [target (larva-render source options)])))

(defn render-assets [assets options]
  ^{:break/when (contains? options :prop)}
  (binding [lnt/*dir* "."]
    (apply lnt/->files options (map #(render-asset larva-render options %)
                                    assets))))

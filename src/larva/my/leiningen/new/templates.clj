(ns larva.my.leiningen.new.templates
  "Some functions from Leiningen that are changed to meet requirements
  of this project. Original namespace can be found on:
  https://github.com/technomancy/leiningen/blob/master/src/leiningen/new/templates.clj"
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [leiningen.core.main :as main]
            [leiningen.new.templates :as original]))

(defn renderer
  "Create a renderer function that looks for mustache templates in the
  right place given the name of your template or path to directory where
  templates are if you use this function for other purposes than
  writing Leiningen templates. If no data is passed, the file is simply slurped
  and the content returned unchanged.
  render-fn - Optional rendering function that will be used in place of the
              default renderer. This allows rendering templates that contain
              tags that conflic with the Stencil renderer such as {{..}}."
  [name & [render-fn templates-path]]
  (let [render (or render-fn original/render-text)]
    (fn [template & [data]]
      (let [path (string/join "/" (if templates-path
                                    [templates-path template]
                                    ["leiningen" "new" (original/sanitize name) template]))]
        (if-let [resource (io/resource path)]
          (if data
            (render (original/slurp-resource resource) data)
            (io/reader resource))
          (main/abort (format "Template resource '%s' not found." path)))))))

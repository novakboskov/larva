(ns larva.frameworks.luminus.stuff
  "All variables and building material specific to a Luminus project.")

(def core-assets
  [[".gitignore" "core/gitignore"]
   ["project.clj" "core/project.clj"]
   ["profiles.clj" "core/profiles.clj"]
   ["Procfile" "core/Procfile"]
   ["README.md" "core/README.md"]
   ["env/dev/resources/log4j.properties" "core/env/dev/resources/log4j.properties"]
   ["env/prod/resources/log4j.properties" "core/env/prod/resources/log4j.properties"]
   ["env/prod/resources/config.edn" "core/env/prod/resources/config.edn"]

   ;; config namespaces
   ["env/dev/clj/{{sanitized}}/config.clj" "core/env/dev/clj/config.clj"]
   ["env/dev/clj/{{sanitized}}/dev_middleware.clj" "core/env/dev/clj/dev_middleware.clj"]
   ["env/prod/clj/{{sanitized}}/config.clj" "core/env/prod/clj/config.clj"]
   ;; core namespaces
   ["env/dev/clj/user.clj" "core/env/dev/clj/user.clj"]
   ["src/{{sanitized}}/core.clj" "core/src/core.clj"]
   ["src/{{sanitized}}/handler.clj" "core/src/handler.clj"]
   ["src/{{sanitized}}/routes/home.clj" "core/src/home.clj"]
   ["src/{{sanitized}}/layout.clj" "core/src/layout.clj"]
   ["src/{{sanitized}}/middleware.clj" "core/src/middleware.clj"]

   ;; HTML templates
   ["resources/templates/base.html" "core/resources/templates/base.html"]
   ["resources/templates/home.html" "core/resources/templates/home.html"]
   ["resources/templates/about.html" "core/resources/templates/about.html"]
   ["resources/templates/error.html" "core/resources/templates/error.html"]

   ;; public resources, example URL: /css/screen.css
   ["resources/public/favicon.ico"  (resource "site/favicon.ico")]
   ["resources/public/css/screen.css" "core/resources/css/screen.css"]
   ["resources/docs/docs.md" "core/resources/docs.md"]
   "resources/public/js"
   "resources/public/img"

   ;; tests
   ["test/{{sanitized}}/test/handler.clj" "core/test/handler.clj"]])
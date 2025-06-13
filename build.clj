(ns build
  (:require [clojure.tools.build.api :as b]
            [babashka.process :as p]
            [clojure.edn :as edn]))

(def project (-> (edn/read-string (slurp "deps.edn"))
                 :aliases :neil :project))
(def lib (:name project))

(def version (:version project))
(def class-dir "target/classes")
(def basis (b/create-basis {:project "deps.edn"}))
(def uber-file (format "target/%s-%s-standalone.jar" (name lib) version))

(defn clean [_]
  (b/delete {:path "target"}))

(defn tailwind [_]
  (p/shell "tailwindcss --minify -i ./src/site/input.css -o ./src/public/site.css"))

(defn uber [_]
  (clean nil)
  (tailwind {:release true})
  (b/copy-dir {:src-dirs   ["src" "resources"]
               :target-dir class-dir})
  (b/compile-clj {:basis     basis
                  :src-dirs  ["src"]
                  :class-dir class-dir})
  (b/uber {:class-dir class-dir
           :uber-file uber-file
           :basis     basis
           :main      'site.server}))

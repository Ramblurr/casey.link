(ns site.pages.urls
  (:require [clojure.string :as str]))

(def paths
  {:url/home          "/"
   :url/about         "/about"
   :url/project-index "/projects"
   :url/blog-index    "/blog"
   :url/blog-post     "/blog/%s"})

(defn url-for
  [path & args]
  (let [path (get paths path)]
    (cond
      (nil? path)
      (throw (ex-info "Unknown path" {:path path}))

      (str/includes? path "%s")
      (apply format path args)

      :else
      path)))

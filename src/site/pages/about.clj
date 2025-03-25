(ns site.pages.about
  (:require [site.ui.about :as about]))

(defn about [_]
  {:title   "About - Casey Link"
   :uri     "/about"
   :content (about/about)})

(comment
  (about nil))
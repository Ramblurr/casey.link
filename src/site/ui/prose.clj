(ns site.ui.prose
  (:require
   [site.ui.core :as uic]))

(defn prose
  [& args]
  (let [[_ attrs children] (uic/extract args)]
    [:div (uic/merge-attrs attrs :class "prose dark:prose-invert")
     children]))

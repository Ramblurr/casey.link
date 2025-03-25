(ns site.ui.prose
  (:require [clojure.string :as str]))

(defn prose
  ([children] (prose children nil))
  ([children class-name]
   [:div {:class (str/join " " ["prose dark:prose-invert" class-name])}
    children]))
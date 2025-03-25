(ns site.ui.container
  (:require [clojure.string :as str]))

(defn container-outer
  ([children] (container-outer children nil))
  ([children class-name]
   [:div {:class (str/join " " ["sm:px-8" class-name])}
    [:div {:class "mx-auto w-full max-w-7xl lg:px-8"}
     children]]))

(defn container-inner
  ([children] (container-inner children nil))
  ([children class-name]
   [:div {:class (str/join " " ["relative px-4 sm:px-8 lg:px-12" class-name])}
    [:div {:class "mx-auto max-w-2xl lg:max-w-5xl"}
     children]]))

(defn container
  ([children] (container children nil))
  ([children class-name]
   (container-outer
    (container-inner children)
    class-name)))
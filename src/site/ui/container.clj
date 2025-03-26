(ns site.ui.container
  (:require
   [site.ui.core :as uic]))

(defn container-outer
  [& args]
  (let [[_ attrs children] (uic/extract args)]
    [:div (uic/merge-attrs attrs :class "sm:px-8")
     [:div {:class "mx-auto w-full max-w-7xl lg:px-8"}
      children]]))

(defn container-inner
  [& args]
  (let [[_ attrs children] (uic/extract args)]
    [:div (uic/merge-attrs attrs :class "relative px-4 sm:px-8 lg:px-12")
     [:div {:class "mx-auto max-w-2xl lg:max-w-5xl"}
      children]]))

(defn container
  [& args]
  (let [[_ attrs children] (uic/extract args)]
    (container-outer attrs
                     (container-inner {} children))))

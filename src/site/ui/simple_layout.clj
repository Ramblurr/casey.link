(ns site.ui.simple-layout
  (:require [site.ui.container :as container]))

(defn simple-layout [{:keys [title intro children]}]
  (container/container
   (list
    [:header {:class "max-w-2xl"}
     [:h1 {:class "text-4xl font-bold tracking-tight text-zinc-800 sm:text-5xl dark:text-zinc-100"}
      title]
     [:p {:class "mt-6 text-base text-zinc-600 dark:text-zinc-400"}
      intro]]
    (when children
      [:div {:class "mt-16 sm:mt-20"}
       children]))
   "mt-16 sm:mt-32"))
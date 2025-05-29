(ns site.ui.article-layout
  (:require [site.ui.container :as container]
            [site.ui.prose :as prose]
            [site.content :refer [format-date]]))

(defn arrow-left-icon
  [class-name]
  [:svg {:viewBox     "0 0 16 16"
         :fill        "none"
         :aria-hidden "true"
         :class       class-name}
   [:path {:d               "M7.25 11.25 3.75 8m0 0 3.5-3.25M3.75 8h8.5"
           :stroke-width    "1.5"
           :stroke-linecap  "round"
           :stroke-linejoin "round"}]])

(defn article-layout
  [{:keys [article children previous-pathname]}]
  [:div {:class "sm:px-8 mt-16 lg:mt-32"}
   [:section {:id "article-container" :class ""}
    [:article {:class "max-w-2xl lg:max-w-5xl"}
     [:header {:class "flex flex-col"}
      [:h1 {:class "mt-6 text-4xl font-bold tracking-tight text-zinc-800 sm:text-5xl dark:text-zinc-100"}
       (:title article)]
      [:time {:datetime (:date article)
              :class    "order-first flex items-center text-base text-zinc-400 dark:text-zinc-500"}
       [:span {:class "h-4 w-0.5 rounded-full bg-zinc-200 dark:bg-zinc-500"}]
       [:span {:class "ml-3"} (format-date (:date article))]]]
     (prose/prose {:class "mt-8"} children)]]])

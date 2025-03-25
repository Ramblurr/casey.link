(ns site.ui.about.social-link
  (:require [clojure.string :as str]))

(defn social-link [{:keys [href class-name icon children]}]
  [:li {:class (str/join " " (filter some? ["flex" class-name]))}
   [:a {:href href
        :class "group flex text-sm font-medium text-zinc-800 transition hover:text-teal-500 dark:text-zinc-200 dark:hover:text-teal-500"}
    (icon {:class "h-6 w-6 flex-none fill-zinc-500 transition group-hover:fill-teal-500"})
    [:span {:class "ml-4"}
     children]]])
(ns site.ui.section
  (:require [clojure.string :as str]))

(defn- generate-id [title]
  (str "section-" (str/lower-case (str/replace title #"\s+" "-"))))

(defn section [{:keys [title children]}]
  (let [id (generate-id title)]
    [:section
     {:aria-labelledby id
      :class "md:border-l md:border-zinc-100 md:pl-6 md:dark:border-zinc-700/40"}
     [:div {:class "grid max-w-3xl grid-cols-1 items-baseline gap-y-8 md:grid-cols-4"}
      [:h2
       {:id id
        :class "text-sm font-semibold text-zinc-800 dark:text-zinc-100"}
       title]
      [:div {:class "md:col-span-3"}
       children]]]))
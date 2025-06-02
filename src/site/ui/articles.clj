(ns site.ui.articles
  (:require [site.ui.simple-layout :as simple-layout]
            [site.ui.home.card :as card]
            [site.content :refer [format-date]]))

(defn article-item
  "Article component for displaying a single article in the articles index page"
  [article]
  [:article {:class "md:grid md:grid-cols-4 md:items-baseline"}
   (card/card {:class "md:col-span-3"}
              (card/title {:-href (str "/articles/" (:slug article))} (:title article))
              (when (:date article)
                (card/eyebrow {:-as        :time
                               :-datetime  (:date article)
                               :class      "md:hidden"
                               :-decorate? true}
                              (format-date (:date article))))
              (card/description (:description article))
              (card/cta "Read article"))
   (when true
     (card/eyebrow {:-as       :time
                    :-datetime (:date article)
                    :class     "mt-1 max-md:hidden"}
                   (format-date (:date article))))])

(defn articles-index
  "Articles index page component"
  [articles]
  (simple-layout/simple-layout
   {:title "The Outskirts Journal"
    :intro "Notes on code, open-source, security, and tech in the social sector."
    :children
    [:div {:class "md:border-l md:border-zinc-100 md:pl-6 md:dark:border-zinc-700/40"}
     [:div {:class "flex max-w-3xl flex-col space-y-16"}
      (for [article-data articles]
        ^{:key (:slug article-data)}
        (article-item article-data))]]}))

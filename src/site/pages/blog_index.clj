(ns site.pages.blog-index
  (:require
   [site.pages.render :as render]
   [site.dev :as dev]
   [site.db :as db]
   [site.pages.blog-post :refer [format-date]]
   [site.pages.urls :refer [url-for]]
   [site.ui.home.card :as card]
   [site.ui.simple-layout :as simple-layout]))

(defn post-item
  "Article component for displaying a single article in the articles index page"
  [{:page/keys [title uri] :blog/keys [date author description]}]
  [:article {:class "md:grid md:grid-cols-4 md:items-baseline"}
   (card/card {:class "md:col-span-3"}
              (card/title {:-href uri} title)
              (when date
                (card/eyebrow {:-as        :time
                               :-datetime  date
                               :class      "md:hidden"
                               :-decorate? true}
                              (format-date date)))
              (card/description description)
              (card/cta "Read article"))
   (when true
     (card/eyebrow {:-as       :time
                    :-datetime date
                    :class     "mt-1 max-md:hidden"}
                   (format-date date)))])

(defn render [req _page]
  {:title "Blog - Casey Link"
   :uri   (url-for :url/blog-index)
   :content
   (simple-layout/simple-layout
    {:title "Blog"
     :intro "Notes on code, open-source, security, and tech in the social sector."
     :children
     [:div {:class "md:border-l md:border-zinc-100 md:pl-6 md:dark:border-zinc-700/40"}
      [:div {:class "flex max-w-3xl flex-col space-y-16"}
       (for [page (db/get-blog-posts (:app/db req))]
         (post-item page))]]})})

(defmethod render/page-content :page.kind/blog-index
  [page req]
  (render req page))

(dev/re-render!)

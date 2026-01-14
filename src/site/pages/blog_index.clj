(ns site.pages.blog-index
  (:require
   [site.db :as db]
   [site.dev :as dev]
   [site.pages.blog-post :refer [format-date]]
   [site.pages.render :as render]
   [site.ui :as ui]
   [site.ui.home.card :as card]
   [site.ui.icons :as icon]
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

(defn render [req {:page/keys [description] :as page}]
  (render/with-body page
    (ui/main
     (simple-layout/simple-layout
      {:title (list "Blog"
                    [:a {:href  "/atom/articles"
                         :title "Subscribe to RSS feed"
                         :class "group ml-3 inline-block align-baseline"}
                     (icon/rss {:class "size-7 sm:size-9 inline-block transition text-zinc-400 fill-zinc-400 group-any-hover:text-ol-orange-500 group-any-hover:fill-ol-orange-500 dark:text-zinc-500 dark:fill-zinc-500 dark:group-any-hover:text-ol-orange-400 dark:group-any-hover:fill-ol-orange-400"})])
       :intro description
       :children
       [:div {:class "md:border-l md:border-zinc-100 md:pl-6 md:dark:border-zinc-700/40"}
        [:div {:class "flex max-w-3xl flex-col space-y-16"}
         (for [page (db/get-blog-posts (:app/db req))]
           (post-item page))]]}))))

(defmethod render/page-content :page.kind/blog-index
  [page req]
  (-> (render req page)
      (assoc
       :page/head (list
                   [:link {:rel   "alternate"
                           :type  "application/atom+xml"
                           :title "Casey Link's Weblog"
                           :href  "/atom/articles"}]))))

(dev/re-render!)

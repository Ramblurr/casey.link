(ns site.pages.blog
  (:require
   [site.content :as content]
   [site.pages.urls :refer [url-for]]
   [site.ui.home.card :as card]
   [site.ui.prose :as prose]
   [site.ui.simple-layout :as simple-layout]))

(defn post-layout
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
       [:span {:class "ml-3"} (content/format-date (:date article))]]]
     (prose/prose {:class "mt-8"} children)]]])

(defn post-item
  "Article component for displaying a single article in the articles index page"
  [article]
  [:article {:class "md:grid md:grid-cols-4 md:items-baseline"}
   (card/card {:class "md:col-span-3"}
              (card/title {:-href (url-for :url/blog-post (:slug article))} (:title article))
              (when (:date article)
                (card/eyebrow {:-as        :time
                               :-datetime  (:date article)
                               :class      "md:hidden"
                               :-decorate? true}
                              (content/format-date (:date article))))
              (card/description (:description article))
              (card/cta "Read article"))
   (when true
     (card/eyebrow {:-as       :time
                    :-datetime (:date article)
                    :class     "mt-1 max-md:hidden"}
                   (content/format-date (:date article))))])

(defn blog-index-layout
  "Articles index page component"
  [articles]
  (simple-layout/simple-layout
   {:title "Blog"
    :intro "Notes on code, open-source, security, and tech in the social sector."
    :children
    [:div {:class "md:border-l md:border-zinc-100 md:pl-6 md:dark:border-zinc-700/40"}
     [:div {:class "flex max-w-3xl flex-col space-y-16"}
      (for [article-data articles]
        ^{:key (:slug article-data)}
        (post-item article-data))]]}))

(defn blog-index-page [_]
  {:title   "Articles - Casey Link"
   :uri     (url-for :url/blog-index)
   :content (blog-index-layout (content/article-index-data))})

(defn article-content
  "Render an article using the ArticleLayout"
  [{:keys [metadata content]}]
  (let [article (cond-> metadata
                  (:date metadata) (assoc :date (:date metadata)))]
    (post-layout
     {:article           article
      :previous-pathname (url-for :url/blog-index)
      :children          content})))

(defn blog-page [{:keys [metadata title content uri path] :as data}]
  {:title   title
   :uri     uri
   :path    path
   :content (article-content data)})

(defn blog-routes
  "Generate reitit routes for all articles"
  [resp-fn]
  (into []
        (map (fn build-article-routes [{:keys [slug dir modified]}]
               [(str "/" slug "/")
                {:handler               (resp-fn (fn [_req]
                                                   (blog-page
                                                    (content/parse-post (url-for :url/blog-pist slug)))))
                 :sitemap/last-modified modified}])
             (content/article-index-data))))


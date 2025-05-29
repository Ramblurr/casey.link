(ns site.pages.articles
  (:require
   [site.ui.articles :as articles]
   [site.ui.article-layout :as article-layout]
   [site.content :as content]))

(defn articles-index [_]
  {:title   "Articles - Casey Link"
   :uri     "/articles"
   :content (articles/articles-index (content/article-index-data))})

(defn article-content
  "Render an article using the ArticleLayout"
  [{:keys [metadata content]}]
  (let [article (cond-> metadata
                  (:date metadata) (assoc :date (:date metadata)))]
    (article-layout/article-layout
     {:article           article
      :previous-pathname "/articles"
      :children          content})))

(defn article-page [{:keys [metadata title content uri] :as data}]
  {:title   title
   :uri     uri
   :content (article-content data)})

(defn article-routes
  "Generate reitit routes for all articles"
  [resp-fn]
  (into []
        (map (fn build-article-routes [{:keys [slug dir modified]}]
               [(str "/" slug "/")
                {:handler               (resp-fn (fn [_req] (content/content article-page "articles" slug)))
                 :sitemap/last-modified modified}])
             (content/article-index-data))))


(ns site.pages.articles
  (:require
   [site.ui.articles :as articles]
   [site.ui.article-layout :as article-layout]
   [site.content :as content]))

(defn articles-index [_]
  {:title   "Articles - Casey Link"
   :uri     "/articles"
   :content (articles/articles-index (content/get-articles))})

(defn article-content
  "Render an article using the ArticleLayout"
  [{:keys [metadata content]}]
  (let [article (assoc metadata :date (:date metadata))]
    (article-layout/article-layout
     {:article           article
      :previous-pathname "/articles"
      :children          content})))

(defn article-page [{:keys [metadata title content uri] :as data}]
  {:title   title
   :uri     uri
   :content (article-content data)})

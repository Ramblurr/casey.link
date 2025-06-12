(ns site.pages
  (:require
   [site.headers :as headers]
   [site.pages.render :as render]
   [site.html :as html]
   [site.polish :as polish]
   [site.ui :as ui]))

(defn get-page-kind [path]
  (cond
    (re-find #"^blog/[a-zA-Z0-9-_]+/index.md" path)
    :page.kind/blog-post

    (re-find #"^about\.md" path)
    :page.kind/about

    :else nil))

(defn fragment-response [config content]
  (when content
    (->  content
         (polish/hiccup config)
         :content
         html/->str)))

(defn html-response [req content]
  (when content
    (let [r (->  content
                 ui/shell
                 (polish/hiccup req)
                 :content
                 html/->str)]
      {:status  200
       :headers headers/default-headers
       :body    r})))

(defn render-fragment [page req]
  (fragment-response req (render/page-content page req)))

(defn render-page [page req]
  (html-response req (render/page-content page req)))

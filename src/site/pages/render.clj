(ns site.pages.render
  (:require
   [site.headers :as headers]
   [site.html :as html]
   [site.polish :as polish]
   [site.ui :as ui]))

(defmulti page-content (fn [page req] (:page/kind page)))

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

(defn page-attrs [page]
  (select-keys page [:page/kind
                     :page/uri
                     :page/resource-path
                     :page/title
                     :page/last-modified
                     :page/description
                     :page/body]))

(defn with-body [page body]
  (-> page
      page-attrs
      (assoc :page/body body)))

(defn render-fragment [page req]
  (fragment-response req (page-content page req)))

(defn render-page [page req]
  (html-response req (page-content page req)))

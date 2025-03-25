(ns site.pages
  (:require
   [site.html :as html]))

(defn shell [page]
  (assoc page :content
         [html/doctype-html5
          [:html {:lang "en"}
           [:head
            [:meta {:http-equiv "content-type" :content "text/html;charset=UTF-8"}]
            [:link {:href "/output.css" :rel "stylesheet" :type "text/css"}]
            [:title (:title page)]]
           [:body (:content page)]]]))

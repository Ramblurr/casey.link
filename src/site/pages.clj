(ns site.pages
  (:require
   [site.html :as html]))

(defn shell [page]
  (assoc page :content
         [html/doctype-html5
          [:html {:lang "en"}
           [:head]
           [:body (:content page)]]]))

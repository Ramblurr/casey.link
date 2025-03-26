(ns site.ui
  (:require
   [site.ui.footer :as footer]
   [site.ui.header :as header]
   [site.html :as html]))

(defn layout [{:keys [uri content] :as page}]
  (list
   [:div {:class "fixed inset-0 flex justify-center sm:px-8"}
    [:div {:class "flex w-full max-w-7xl lg:px-8"}
     [:div {:class "w-full bg-white ring-1 ring-zinc-100 dark:bg-stone-800 dark:ring-zinc-300/20"}]]]
   [:div {:class "relative flex w-full flex-col"}
    (header/header {:path uri})
    [:main {:class "flex-auto"} content]
    (footer/footer)]))

(defn shell [{:keys [uri content] :as page}]
  (assoc page :content
         [html/doctype-html5
          [:html {:lang "en" :class "h-full antialiased"}
           [:head
            [:meta {:http-equiv "content-type" :content "text/html;charset=UTF-8"}]
            [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
            [:link {:href "/site.css" :rel "stylesheet" :type "text/css"}]
            [:title (:title page)]]
           [:body {:class "flex h-full bg-stone-200 dark:bg-stone-900"}
            [:div {:class "flex w-full"}
             (layout page)]
            [:script {:src "/js/navigation.js"}]]]]))

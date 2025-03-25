(ns site.ui
  (:require
   [site.ui.header :as header]
   [site.html :as html]))

(defn nav-link
  "Navigation link component"
  [{:keys [href]} & children]
  [:a {:href  href
       :class "transition hover:text-ol-orange dark:hover:text-ol-orange"}
   children])

(defn footer
  "Footer component"
  []
  [:footer {:class "mt-32 flex-none"}
   [:div {:id "container-outer"}
    [:div {:class "border-t border-ol-light-gray/10 pt-10 pb-16 dark:border-ol-light-gray/20"}
     [:div {:id "container-inner"}
      [:div {:class "flex flex-col items-center justify-between gap-6 md:flex-row"}
       [:div {:class "flex flex-wrap justify-center gap-x-6 gap-y-1 text-sm font-medium text-ol-gray dark:text-white"}
        (nav-link {:href "/about"} "About")
        (nav-link {:href "/articles"} "Articles")
        (nav-link {:href "/projects"} "Projects")
        (nav-link {:href "/speaking"} "Speaking")
        (nav-link {:href "/uses"} "Uses")]
       [:p {:class "text-sm text-ol-light-gray dark:text-ol-light-gray"}

        (str "© 2009–" (.getValue (java.time.Year/now)) " Casey Link | Outskirts Labs e.U. All rights reserved.")]]]]]])

(defn layout [children]
  (list
   [:div {:classname "fixed inset-0 flex justify-center sm:px-8"}
    [:div {:classname "flex w-full max-w-7xl lg:px-8"}
     [:div {:classname "w-full bg-white ring-1 ring-zinc-100 dark:bg-zinc-900 dark:ring-zinc-300/20"}]]]
   [:div {:classname "relative flex w-full flex-col"}
    (header/header)
    [:main {:classname "flex-auto"} children]
    (footer)]))

(defn shell [page]
  (assoc page :content
         [html/doctype-html5
          [:html {:lang "en" :class "h-full antialiased"}
           [:head
            [:meta {:http-equiv "content-type" :content "text/html;charset=UTF-8"}]
            [:link {:href "/site.css" :rel "stylesheet" :type "text/css"}]
            [:title (:title page)]]
           [:body {:class "flex h-full bg-white dark:bg-ol-gray-dark"}
            [:div {:class "flex w-full"}
             (layout (:content page))]]]]))

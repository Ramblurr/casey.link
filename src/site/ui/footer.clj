(ns site.ui.footer
  (:require [site.ui.container :as container]
            [site.ui.icons :as icon]
            [site.pages.urls :as urls]))

(defn nav-link
  "Navigation link component"
  [{:keys [href]} & children]
  [:a {:href  href
       :class "transition any-hover:text-ol-orange dark:any-hover:text-ol-orange"}
   children])

(defn footer
  "Footer component"
  []
  [:footer {:class "mt-32 flex-none"}
   (container/container-outer
    [:div {:class "border-t border-ol-light-gray/10 pt-10 pb-16 dark:border-ol-light-gray/20"}
     (container/container-inner
      [:div {:class "flex flex-col items-center justify-between gap-6 md:flex-row"}
       [:div {:class "flex flex-wrap justify-center gap-x-6 gap-y-1 text-sm font-medium text-ol-gray dark:text-white"}
        (nav-link {:href (urls/url-for :url/home)}
                  (icon/house {:class "inline text-inherit size-4 align-text-bottom" :aria-label "Home"}))
        (nav-link {:href (urls/url-for :url/about)} "About")
        (nav-link {:href (urls/url-for :url/blog-index)} "Articles")
        (nav-link {:href (urls/url-for :url/project-index)} "Projects")]
       [:p {:class "text-sm text-ol-light-gray dark:text-ol-light-gray"}
        "Thank you for visiting this personal website."
        [:br]
        [:span {:class "text-xs"}
         (str "© 2009–" (.getValue (java.time.Year/now)) " Casey Link")]]])])])

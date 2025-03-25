(ns site.ui.footer)

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

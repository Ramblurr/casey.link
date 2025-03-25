(ns site.ui.header)

(defn close-icon
  "Close icon SVG component"
  [{:keys [class]}]
  [:svg {:viewBox "0 0 24 24"
         :aria-hidden "true"
         :class class}
   [:path {:d "m17.25 6.75-10.5 10.5M6.75 6.75l10.5 10.5"
           :fill "none"
           :stroke "currentColor"
           :stroke-width "1.5"
           :stroke-linecap "round"
           :stroke-linejoin "round"}]])

(defn chevron-down-icon
  "Chevron down icon SVG component"
  [{:keys [class]}]
  [:svg {:viewBox "0 0 8 6"
         :aria-hidden "true"
         :class class}
   [:path {:d "M1.75 1.75 4 4.25l2.25-2.5"
           :fill "none"
           :stroke "currentColor"
           :stroke-width "1.5"
           :stroke-linecap "round"
           :stroke-linejoin "round"}]])

(defn sun-icon
  "Sun icon SVG component"
  [{:keys [class]}]
  [:svg {:viewBox "0 0 24 24"
         :stroke-width "1.5"
         :stroke-linecap "round"
         :stroke-linejoin "round"
         :aria-hidden "true"
         :class class}
   [:path {:d "M8 12.25A4.25 4.25 0 0 1 12.25 8v0a4.25 4.25 0 0 1 4.25 4.25v0a4.25 4.25 0 0 1-4.25 4.25v0A4.25 4.25 0 0 1 8 12.25v0Z"}]
   [:path {:d "M12.25 3v1.5M21.5 12.25H20M18.791 18.791l-1.06-1.06M18.791 5.709l-1.06 1.06M12.25 20v1.5M4.5 12.25H3M6.77 6.77 5.709 5.709M6.77 17.73l-1.061 1.061"
           :fill "none"}]])

(defn moon-icon
  "Moon icon SVG component"
  [{:keys [class]}]
  [:svg {:viewBox "0 0 24 24"
         :aria-hidden "true"
         :class class}
   [:path {:d "M17.25 16.22a6.937 6.937 0 0 1-9.47-9.47 7.451 7.451 0 1 0 9.47 9.47ZM12.75 7C17 7 17 2.75 17 2.75S17 7 21.25 7C17 7 17 11.25 17 11.25S17 7 12.75 7Z"
           :stroke "currentColor"
           :stroke-width "1.5"
           :stroke-linecap "round"
           :stroke-linejoin "round"}]])

(defn container-outer
  "Outer container component"
  [{:keys [class style]} & children]
  [:div {:class (str "sm:px-8 " class)
         :style style}
   [:div {:class "mx-auto w-full max-w-7xl lg:px-8"}
    (when (seq children) children)]])

(defn container-inner
  "Inner container component"
  [{:keys [class]} & children]
  [:div {:class (str "relative px-4 sm:px-8 lg:px-12 " class)}
   [:div {:class "mx-auto max-w-2xl lg:max-w-5xl"}
    (when (seq children) children)]])

(defn container
  "Container component combining outer and inner containers"
  [{:keys [class style]} & children]
  (container-outer {:class class :style style}
                   (apply container-inner {} children)))

(defn mobile-nav-item
  "Mobile navigation item component"
  [{:keys [href]} & children]
  [:li
   [:a {:href  href
        :class "block py-2"}
    children]])

(defn mobile-navigation
  "Mobile navigation component"
  [{:keys [class]}]
  [:div {:class (str "relative " class)}
   [:button {:type "button"
             :class "group flex items-center rounded-full bg-white/90 px-4 py-2 text-sm font-medium text-ol-gray shadow-lg ring-1 shadow-ol-gray/5 ring-ol-gray/5 backdrop-blur-sm dark:bg-ol-gray/90 dark:text-white dark:ring-white/10 dark:hover:ring-white/20"
             :aria-expanded "false"
             :aria-controls "mobile-nav-panel"
             :data-js "mobile-nav-button"}
    "Menu"
    (chevron-down-icon {:class "ml-3 h-auto w-2 stroke-zinc-500 group-hover:stroke-zinc-700 dark:group-hover:stroke-zinc-400"})]
   [:div {:id "mobile-nav-panel"
          :class "fixed inset-x-4 top-8 z-50 origin-top rounded-3xl bg-white p-8 ring-1 ring-ol-gray/5 dark:bg-ol-gray dark:ring-ol-light-gray/20 hidden"
          :data-js "mobile-nav-panel"}
    [:div {:class "flex flex-row-reverse items-center justify-between"}
     [:button {:aria-label "Close menu"
               :class "-m-1 p-1"
               :data-js "mobile-nav-close"}
      (close-icon {:class "h-6 w-6 text-zinc-500 dark:text-zinc-400"})]
     [:h2 {:class "text-sm font-medium text-ol-light-gray dark:text-ol-light-gray"}
      "Navigation"]]
    [:nav {:class "mt-6"}
     [:ul {:class "-my-2 divide-y divide-ol-light-gray/10 text-base text-ol-gray dark:divide-ol-light-gray/10 dark:text-white"}
      (mobile-nav-item {:href "/about"} "About")
      (mobile-nav-item {:href "/articles"} "Articles")
      (mobile-nav-item {:href "/projects"} "Projects")
      (mobile-nav-item {:href "/speaking"} "Speaking")
      (mobile-nav-item {:href "/uses"} "Uses")]]]])

(defn nav-item
  "Navigation item component"
  [{:keys [href active?]} & children]
  [:li
   [:a {:href href
        :class (str "relative block px-3 py-2 transition "
                    (if active?
                      "text-ol-orange dark:text-ol-orange"
                      "hover:text-ol-orange dark:hover:text-ol-orange"))}
    children
    (when active?
      [:span {:class "absolute inset-x-1 -bottom-px h-px bg-linear-to-r from-ol-orange/0 via-ol-orange/40 to-ol-orange/0 dark:from-ol-orange/0 dark:via-ol-orange/40 dark:to-ol-orange/0"}])]])

(defn desktop-navigation
  "Desktop navigation component"
  [{:keys [class path]}]
  [:nav {:class class}
   [:ul {:class "flex rounded-full bg-white/90 px-3 text-sm font-medium text-ol-gray shadow-lg ring-1 shadow-ol-gray/5 ring-ol-gray/5 backdrop-blur-sm dark:bg-ol-gray/90 dark:text-white dark:ring-white/10"}
    (nav-item {:href "/about" :active? (= path "/about")} "About")
    (nav-item {:href "/articles" :active? (= path "/articles")} "Articles")
    (nav-item {:href "/projects" :active? (= path "/projects")} "Projects")
    (nav-item {:href "/speaking" :active? (= path "/speaking")} "Speaking")
    (nav-item {:href "/uses" :active? (= path "/uses")} "Uses")]])

(defn theme-toggle
  "Theme toggle button"
  []
  [:button {:type "button"
            :aria-label "Toggle theme"
            :class "group rounded-full bg-white/90 px-3 py-2 shadow-lg ring-1 shadow-ol-gray/5 ring-ol-gray/5 backdrop-blur-sm transition dark:bg-ol-gray/90 dark:ring-white/10 dark:hover:ring-white/20"
            :data-js "theme-toggle"}
   (sun-icon {:class "h-6 w-6 fill-ol-light-gray/20 stroke-ol-light-gray transition group-hover:fill-ol-light-gray/30 group-hover:stroke-ol-gray dark:hidden [@media(prefers-color-scheme:dark)]:fill-turquoise/20 [@media(prefers-color-scheme:dark)]:stroke-turquoise [@media(prefers-color-scheme:dark)]:group-hover:fill-turquoise/30 [@media(prefers-color-scheme:dark)]:group-hover:stroke-turquoise"})
   (moon-icon {:class "hidden h-6 w-6 fill-ol-gray stroke-ol-light-gray transition dark:block [@media_not_(prefers-color-scheme:dark)]:fill-turquoise/10 [@media_not_(prefers-color-scheme:dark)]:stroke-turquoise [@media(prefers-color-scheme:dark)]:group-hover:stroke-white"})])

(defn logo
  "Logo component"
  [{:keys [large? class style]}]
  [:a {:href "/"
       :aria-label "Home"
       :class (str "pointer-events-auto " class)}
   [:div {:class (str "relative overflow-hidden bg-transparent "
                      (if large? "h-16 w-52" "h-9 w-40"))
          :style style}
    [:img {:src "/vector/logo-with-orange-text.svg"
           :alt "Outskirts Labs Logo"
           :class "object-contain dark:hidden"}]
    [:img {:src "/vector/logo-with-orange-text-dark-mode.svg"
           :alt "Outskirts Labs Logo"
           :class "object-contain hidden dark:block"}]]])

(defn avatar-container
  "Avatar container component"
  [{:keys [class style]}]
  [:div {:class (str "h-10 w-32 rounded-full bg-white/90 p-0.5 shadow-lg ring-1 shadow-zinc-800/5 ring-zinc-900/5 backdrop-blur-sm dark:bg-zinc-800/90 dark:ring-white/10 " class)
         :style style}])

(defn header
  "Header component"
  ([] (header {}))
  ([{:keys [path]}]
   (let [is-home-page? (= path "/")]
     (list
      [:header {:id    "main-header"
                :class "pointer-events-none relative z-50 flex flex-none flex-col"
                :style {:height        "var(--header-height, auto)"
                        :margin-bottom "var(--header-mb, 0)"}}
       (when is-home-page?
         (list
          [:div {:id    "avatar-ref"
                 :class "order-last mt-16"}]
          (container {:class "top-0 order-last -mb-3 pt-3"
                      :style {:position "var(--header-position, sticky)"}}
                     [:div {:class "top-[var(--avatar-top,0.75rem)] w-full"
                            :style {:position "var(--header-inner-position, relative)"}}
                      [:div {:class "relative"}
                       (avatar-container {:id    "avatar-container"
                                          :class "absolute top-3 left-0 origin-left transition-opacity"
                                          :style {:opacity   "var(--avatar-border-opacity, 0)"
                                                  :transform "var(--avatar-border-transform, none)"}})
                       (logo {:id     "home-logo"
                              :large? true
                              :class  "ml-0.5 block origin-left"
                              :style  {:transform "var(--avatar-image-transform, none)"}})]])))
       [:div {:id    "header-ref"
              :class "top-0 z-10 h-16 pt-6"
              :style {:position "var(--header-position, sticky)"}}
        (container {:class "top-[var(--header-top,1.5rem)] w-full"
                    :style {:position "var(--header-inner-position, relative)"}}
                   [:div {:class "relative flex gap-4"}
                    [:div {:class "flex flex-1"}
                     (when-not is-home-page?
                       [:div {:class "flex items-center"}
                        (logo {:id "page-logo"})])]
                    [:div {:class "flex flex-1 justify-end md:justify-center"}
                     (mobile-navigation {:class "pointer-events-auto md:hidden"})
                     (desktop-navigation {:class "pointer-events-auto hidden md:block" :path path})]
                    [:div {:class "flex justify-end md:flex-1"}
                     [:div {:class "pointer-events-auto"}
                      (theme-toggle)]]])]]
      (when is-home-page?
        [:div {:id    "content-offset"
               :class "flex-none"
               :style "height: var(--content-offset, 0);"}])))))

(ns site.ui.header
  (:require
   [site.ui.icons :as icon]
   [site.ui.core :as uic]
   [site.ui.container :as container]))

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
   [:button {:type          "button"
             :class         "group flex items-center rounded-full bg-white/90 px-4 py-2 text-sm font-medium text-stone-800 ring-1 shadow-lg shadow-stone-800/5 ring-stone-900/5 backdrop-blur-sm dark:bg-stone-800/90 dark:text-stone-200 dark:ring-white/10 dark:hover:ring-white/20"
             :popovertarget "mobile-nav-panel"}
    "Menu"
    (icon/chevron-down {:class "ml-3 h-auto w-2 stroke-stone-500 group-hover:stroke-stone-700 dark:group-hover:stroke-stone-400"})]
   [:div {:id      "mobile-nav-panel"
          :popover true
          :class   (uic/cs "fixed w-auto inset-x-4 top-8 z-50 origin-top rounded-3xl bg-white p-8 ring-1 ring-stone-900/5 dark:bg-stone-900 dark:ring-stone-800"
                           "backdrop:backdrop-blur-xs opacity-0 transition-[display,opacity] transition-discrete duration-150 open:opacity-100 motion-reduce:duration-0 starting:open:opacity-0")

          :data-ref "navMenuPopover"}
    [:div {:class "flex flex-row-reverse items-center justify-between"}
     [:button {:aria-label    "Close menu"
               :class         "-m-1 p-1"
               :data-on-click "$navMenuPopover.hidePopover()"}
      (icon/close {:class "h-6 w-6 text-stone-500 dark:text-stone-400"})]
     [:h2 {:class "text-sm font-medium text-stone-600 dark:text-stone-400"}
      "Navigation"]]
    [:nav {:class "mt-6"}
     [:ul {:class "-my-2 divide-y divide-stone-100 text-base text-stone-800 dark:divide-stone-100/5 dark:text-stone-300"}
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
   [:ul {:class
         "flex rounded-full bg-white/90 px-3 text-sm font-medium text-stone-800 ring-1 shadow-lg shadow-stone-800/5 ring-stone-900/5 backdrop-blur-sm dark:bg-stone-800/90 dark:text-stone-200 dark:ring-white/10"}
    (nav-item {:href "/about" :active? (= path "/about")} "About")
    (nav-item {:href "/articles" :active? (= path "/articles")} "Articles")
    (nav-item {:href "/projects" :active? (= path "/projects")} "Projects")
    (nav-item {:href "/speaking" :active? (= path "/speaking")} "Speaking")
    (nav-item {:href "/uses" :active? (= path "/uses")} "Uses")]])

(defn theme-toggle
  "Theme toggle button"
  []
  [:button {:type          "button"
            :aria-label    "Toggle theme"
            :class         "cursor-pointer group rounded-full bg-white/90 px-3 py-2 ring-1 shadow-lg shadow-stone-800/5 ring-stone-900/5 backdrop-blur-sm transition dark:bg-stone-800/90 dark:ring-white/10 dark:hover:ring-white/20"
            :data-on-click "$darkmode = !$darkmode"}
   (icon/sun {:class "h-6 w-6 fill-stone-100 stroke-stone-500 transition group-hover:fill-stone-200 group-hover:stroke-stone-700 dark:hidden [@media(prefers-color-scheme:dark)]:fill-teal-50 [@media(prefers-color-scheme:dark)]:stroke-teal-500 [@media(prefers-color-scheme:dark)]:group-hover:fill-teal-50 [@media(prefers-color-scheme:dark)]:group-hover:stroke-teal-600"})
   (icon/moon-sparkle {:class "hidden h-6 w-6 fill-stone-700 stroke-stone-500 transition dark:block [@media_not_(prefers-color-scheme:dark)]:fill-teal-400/10 [@media_not_(prefers-color-scheme:dark)]:stroke-teal-500 [@media(prefers-color-scheme:dark)]:group-hover:stroke-stone-400"})])

(defn logo
  [& args]
  (let [[opts attrs children] (uic/extract args)
        {:keys [large?]}      opts
        alt                   "Outskirts Labs' logo, a laboratory flask with a bubbling cactus inside."]
    [:a (uic/merge-attrs attrs
                         :href       "/"
                         :aria-label "Home"
                         :class      "pointer-events-auto")
     (icon/flask {:id    "logo-square"
                  :class (uic/cs
                          "rounded-full object-cover"
                          (if large? "h-22 w-22 " "h-9 w-9"))
                  :alt   alt})]))

(defn logo-background
  "Avatar container component"
  [& args]
  (let [[_ attrs children] (uic/extract args)]
    [:div (uic/merge-attrs attrs :class
                           "h-10 w-10 rounded-full bg-white/90 p-0.5 ring-1 shadow-lg shadow-stone-800/5 ring-stone-900/5 backdrop-blur-sm dark:bg-stone-800/90 dark:ring-white/10")
     children]))

(defn header
  "Header component"
  [{:keys [path]}]
  (let [is-home-page? (= path "/")]
    (list
     [:header {:id    "main-header"
               :class "pointer-events-none relative z-50 flex flex-none flex-col"
               :style {:height        "var(--header-height, auto)"
                       :margin-bottom "var(--header-mb, 0)"}}
      (when is-home-page?
        (list
         [:div {:id    "avatar-ref"
                :class "order-last mt-16 mt-[calc(--spacing(16)-(--spacing(3)))]"}]
         (container/container {:class "top-0 order-last -mb-3 pt-3"
                               :style {:position "var(--header-position)"}}
                              [:div {:class "top-[var(--avatar-top,0.75rem)] w-full"
                                     :style {:position "var(--header-inner-position, relative)"}}
                               [:div {:class "relative"}
                                (logo-background {:id    "logo-background"
                                                  :class "absolute top-3 left-0 origin-left transition-opacity"
                                                  :style {:opacity   "var(--avatar-border-opacity, 0)"
                                                          :transform "var(--avatar-border-transform)"}})
                                (logo {:-large? true
                                       :id      "home-logo"
                                       :class   "block h-16 w-16 origin-left"
                                       :style   {:translate "var(--avatar-translate)"
                                                 :transform "var(--avatar-image-transform)"}})]])))
      [:div {:id    "header-ref"
             :class "top-0 z-10 h-16 pt-6"
             :style {:position "var(--header-position, sticky)"}}
       (container/container {:class "top-[var(--header-top,1.5rem)] w-full"
                             :style {:position "var(--header-inner-position, relative)"}}
                            [:div {:class "relative flex gap-4"}
                             [:div {:class "flex flex-1"}
                              (when-not is-home-page?
                                [:div {:class "flex items-center"}
                                 (logo-background {:id "logo-background" :class "flex justify-center"}
                                                  (logo {:id "page-logo"}))])]
                             [:div {:class "flex flex-1 justify-end md:justify-center"}
                              (mobile-navigation {:class "pointer-events-auto md:hidden"})
                              (desktop-navigation {:class "pointer-events-auto hidden md:block" :path path})]
                             [:div {:class "flex justify-end md:flex-1"}
                              [:div {:class "pointer-events-auto"}
                               (theme-toggle)]]])]]
     (when is-home-page?
       [:div {:id    "content-offset"
              :class "flex-none"
              :style "height: var(--content-offset, 0);"}]))))

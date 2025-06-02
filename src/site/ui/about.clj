(ns site.ui.about
  (:require [site.ui.container :as container]
            [site.ui.icons :as icon]
            [site.ui.core :as uic]
            [site.content :as content]))

(defn social-link [{:keys [href class-name icon children]}]
  [:li {:class (uic/cs "flex" class-name)}
   [:a {:href  href
        :class "group flex text-sm font-medium text-stone-800 transition hover:text-ol-turquoise-600 dark:text-stone-200 dark:hover:text-ol-turquoise-500"}
    (icon {:class "h-6 w-6 flex-none fill-stone-500 transition group-hover:fill-ol-turquoise-500"})
    [:span {:class "ml-4 transition"}
     children]]])

(defn about []
  (let [[{:keys [title]} content] (content/get-about)]
    (container/container  {:class "mt-16 sm:mt-32"}
                          [:div {:class "grid grid-cols-1 gap-y-16 lg:grid-cols-2 lg:grid-rows-[auto_1fr] lg:gap-y-12"}
                           [:div {:class "lg:pl-20"}
                            [:div {:class "max-w-xs px-2.5 lg:max-w-none"}
                             [:img {:src   "/images/photos/portrait@2x.webp"
                                    :alt   ""
                                    :class "aspect-square rotate-3 rounded-2xl bg-stone-100 object-cover dark:bg-stone-800"}]]]
                           [:article {:class "sidenote-gutter-lg lg:order-first lg:row-span-2"}
                            [:h1 {:class "text-4xl font-bold tracking-tight text-stone-800 sm:text-5xl dark:text-stone-100"}
                             title]
                            [:div {:class "mt-6 space-y-7 text-base text-stone-600 dark:text-stone-400"}
                             [:div {:class "prose dark:prose-invert"}
                              content]]]
                           [:div {:class "lg:pl-20"}
                            [:ul {:role "list"}
                             [:li {:class ""}
                              [:span {:class "group flex text-sm font-medium text-stone-800"}
                               (icon/at {:class "h-6 w-6 flex-none fill-stone-500 transition group-hover:fill-ol-turquoise-500"}) "Ramblurr"
                               [:small {:class "ml-1 transition"}
                                "(is my handle all over the web)"]]]
                             #_(social-link {:href       "https://twitter.com/ramblurr"
                                             :icon       icon/the-social-network-formerly-known-as-twitter
                                             :class-name "mt-4"
                                             :children   "Follow on X"})
                             (social-link {:href       "https://bsky.app/profile/ramblurr.bsky.social"
                                           :icon       icon/bluesky-outline
                                           :class-name "mt-4"
                                           :children   "Follow on Bluesky"})
                             (social-link {:href       "https://github.com/ramblurr"
                                           :icon       icon/github
                                           :class-name "mt-4"
                                           :children   "Follow on GitHub"})
                             (social-link {:href       "https://matrix.to/#/@ramblurr:outskirtslabs.com"
                                           :icon       icon/matrix
                                           :class-name "mt-4"
                                           :children   "Chat on Matrix"})
                             (social-link {:href       "mailto:casey@outskirtslabs.com"
                                           :icon       icon/envelope
                                           :class-name "mt-8 border-t border-stone-100 pt-8 dark:border-stone-700/40"
                                           :children   "casey@outskirtslabs.com"})]]])))

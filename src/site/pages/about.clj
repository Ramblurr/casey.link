(ns site.pages.about
  (:require
   [clojure.string :as str]
   [site.dev :as dev]
   [site.markdown :as md]
   [site.pages.render :as render]
   [site.pages.urls :as urls]
   [site.ui :as ui]
   [site.ui.container :as container]
   [site.ui.core :as uic]
   [site.ui.home.card :as card]
   [site.ui.icons :as icon]))

(defn social-link [{:keys [href class-name icon children]}]
  [:li {:class (uic/cs "flex" class-name)}
   [:a {:href  href
        :class "group flex text-sm font-medium text-stone-800 transition any-hover:text-ol-turquoise-600 dark:text-stone-200 dark:any-hover:text-ol-turquoise-500"}
    (icon {:class "h-6 w-6 flex-none fill-stone-500 transition group-any-hover:fill-ol-turquoise-500"})
    [:span {:class "ml-4 transition"}
     children]]])

(def photo "/images/photos/portrait-300w.webp")

(defn srcset [sizes path ext name]
  (str/join ",\n"
            (for [size sizes]
              (str path name "-" size "w" ext " " size "w"))))

(def portrait-srcset (srcset [300 600 900 1236]
                             "/images/photos/"
                             ".webp"
                             "portrait"))
(def portrait-sizes "(min-width: 1320px) 412px, (min-width: 1060px) calc(39.17vw - 97px), (min-width: 400px) 295px, calc(65vw + 53px)")
(defn render [_req {:page/keys [body description] :as page}]
  (render/with-body page
    (ui/main
     (container/container  {:class "mt-16 sm:mt-32"}
                           [:div {:class "grid grid-cols-1 gap-y-16 lg:grid-cols-2 lg:grid-rows-[auto_1fr] lg:gap-y-12"}
                            [:div {:class "lg:pl-20"}
                             [:div {:class "max-w-xs px-2.5 lg:max-w-none"}
                              [:img {:src    photo
                                     :sizes  portrait-sizes
                                     :srcset portrait-srcset
                                     :alt    "A portrait of me, a man in glasses and cap smiling with black and white dog resting paws up on his shoulders"
                                     :class  "aspect-square rotate-3 rounded-2xl bg-stone-100 object-cover dark:bg-stone-800"}]]]
                            [:article {:class "sidenote-gutter-lg lg:order-first lg:row-span-2"}
                             [:h1 {:class "text-4xl font-bold tracking-tight text-stone-800 sm:text-5xl dark:text-stone-100"}
                              description]
                             [:div {:class "mt-6 space-y-7 text-base text-stone-600 dark:text-stone-400"}
                              [:div {:class "prose dark:prose-invert"}
                               (md/->hiccup body)]
                              [:div
                               (card/card {:class "md:col-span-3"}
                                          (card/title {:-href (urls/url-for :url/blog-index)} "Still Interested?")
                                          (card/description "Check out my articles where I write about software, consultancy, and occasionally the intersection of tech and real life.")
                                          (card/cta "To the articles"))]]]
                            [:div {:class "lg:pl-20"}
                             [:ul {:role "list"}
                              [:li {:class ""}
                               [:span {:class "group flex text-sm font-medium text-stone-800 dark:text-stone-200"}
                                (icon/at {:class "h-6 w-6 flex-none fill-stone-500 transition group-any-hover:fill-ol-turquoise-500"}) "Ramblurr"
                                [:small {:class "ml-1 transition"}
                                 "(is my handle all over the web)"]]]
                              #_(social-link {:href       "https://twitter.com/ramblurr"
                                              :icon       icon/the-social-network-formerly-known-as-twitter
                                              :class-name "mt-4"
                                              :children   "Follow on X"})
                              (social-link {:href       "https://bsky.app/profile/casey.link"
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
                                            :children   "casey@outskirtslabs.com"})
                              (social-link {:href       "/pgp.asc"
                                            :icon       icon/key
                                            :class-name "mt-4"
                                            :children   "PGP Key"})]]]))))

(defmethod render/page-content :page.kind/about
  [page req]
  (-> (render req page)
      (assoc
       :page/head (list
                   [:link {:rel         "preload"
                           :href        photo
                           :as          "image"
                           :imagesrcset portrait-srcset
                           :imagesizes  portrait-sizes}]))))

(dev/re-render!)

(ns site.ui.home
  (:require
   [site.ui.icons :as icon]
   [site.ui.container :as container]
   [site.ui.home.card :as card]
   [site.ui.home.button :as button])
  (:import (java.time Year)))

(defn arrow-down-icon
  "Arrow down icon SVG component"
  [{:keys [class]}]
  [:svg {:viewBox     "0 0 16 16"
         :fill        "none"
         :aria-hidden "true"
         :class       class}
   [:path {:d               "M4.75 8.75 8 12.25m0 0 3.25-3.5M8 12.25v-8.5"
           :stroke-width    "1.5"
           :stroke-linecap  "round"
           :stroke-linejoin "round"}]])

;; Main Component Functions
(defn article
  "Article component for displaying a single article"
  [{:keys [title slug date description]}]
  (card/card {:as :article}
             (card/title {:-href (str "/blog/" slug)} title)
             (card/eyebrow {:-as :time :-datetime date :-decorate? true} date)
             (card/description description)
             (card/cta "Read article")))

(defn social-link
  "Social link component for a single social media link"
  [{:keys [href aria-label icon]}]
  [:a {:href       href
       :aria-label aria-label
       :class      "group -m-1 p-1"}
   (icon {:class "h-6 w-6 transition fill-stone-400 group-hover:fill-ol-orange-600 dark:fill-stone-400 dark:group-hover:fill-ol-orange-500
text-stone-400 group-hover:text-ol-orange-600 dark:text-stone-400 dark:group-hover:text-ol-orange-500
"})])

#_(defn newsletter
    "Newsletter signup component"
    []
    [:form {:action "/thank-you"
            :class  "rounded-2xl border border-ol-light-gray/10 p-6 dark:border-ol-light-gray/20"}
     [:h2 {:class "flex text-sm font-semibold text-ol-gray dark:text-white"}
      (icon/envelope {:class "h-6 w-6 flex-none text-stone-400 dark:text-stone-500"})
      [:span {:class "ml-3"} "Stay up to date"]]
     [:p {:class "mt-2 text-sm text-stone-800 dark:text-stone-100"}
      "Get notified when I publish new articles, project insights, or technical resources."]
     [:div {:class "mt-6 flex"}
      [:input {:type        "email"
               :placeholder "Email address"
               :aria-label  "Email address"
               :required    true
               :class       "min-w-0 flex-auto appearance-none rounded-md border border-ol-gray/10 bg-white px-3 py-[calc(--spacing(2)-1px)] shadow-md shadow-ol-gray/5 placeholder:text-stone-100 focus:border-ol-orange focus:ring-4 focus:ring-ol-orange/10 focus:outline-hidden sm:text-sm dark:border-ol-light-gray/20 dark:bg-ol-gray-dark/[0.25] dark:text-white dark:placeholder:text-stone-100-dark dark:focus:border-ol-orange-dark dark:focus:ring-ol-orange-dark/10"}]
      (button/button {:type "submit" :class "ml-4 flex-none"} "Join")]])

(defn role
  "Role component for a single job/position"
  [{:keys [company title icon start end link]}]
  (let [start-label (if (string? start) start (:label start))
        start-date  (if (string? start) start (:datetime start))
        end-label   (if (string? end) end (:label end))
        end-date    (if (string? end) end (:datetime end))]
    [:li {:class "flex gap-4"}
     [(if link :a :div) {:href link :class "relative mt-1 flex h-10 w-10 flex-none items-center justify-center rounded-full shadow-md ring-1 shadow-ol-gray/5 ring-ol-gray/5 dark:border dark:border-ol-light-gray/20 dark:bg-ol-gray dark:ring-0"}
      (icon {:class "size-7" :alt (str "Logo for " company) :role "img"})]
     [:dl {:class "flex flex-auto flex-wrap gap-x-2"}
      [:dt {:class "sr-only"} "Company"]
      [:dd {:class "w-full flex-none text-sm font-medium text-ol-gray dark:text-white"}
       (if link
         [:a {:href link :class "hover:bg-stone-100 dark:hover:bg-stone-900/50"} company]
         company)]
      [:dt {:class "sr-only"} "Role"]
      [:dd {:class "text-xs text-stone-600 dark:text-stone-100"}
       title]
      [:dt {:class "sr-only"} "Date"]
      [:dd {:class      "ml-auto text-xs text-stone-600 dark:text-stone-100"
            :aria-label (str start-label " until " end-label)}
       [:time {:datetime start-date} start-label] "–" [:time {:datetime end-date} end-label]]]]))

(defn resume
  "Resume component showing work experience"
  []
  (let [current-year (.getValue (Year/now))
        resume-data  [{:company "Outskirts Labs"
                       :link    "https://outskirtslabs.com"
                       :title   "Principal Consultant"
                       :icon    icon/flask
                       :start   "2017"
                       :end     {:label    "Present"
                                 :datetime (str current-year)}}
                      {:company "Cropster GmbH"
                       :title   "Software Engineer"
                       :link    "https://cropster.com"
                       :icon    icon/cropster
                       :start   "2015"
                       :end     "2017"}
                      {:company "Outskirts Labs"
                       :title   "Freelancer"
                       :link    "https://outskirtslabs.com"
                       :icon    icon/flask
                       :start   "2012"
                       :end     "2015"}
                      {:company "KDAB"
                       :title   "Software Engineer"
                       :link    "https://kdab.com"
                       :icon    icon/kdab
                       :start   "2010"
                       :end     "2012"}
                      {:company "KDE"
                       :title   "Sponsored Developer"
                       :icon    icon/kde
                       :start   "2006"
                       :end     "2011"}]]
    [:div {:class "rounded-2xl border border-ol-light-gray/10 p-6 dark:border-ol-light-gray/20"}
     [:h2 {:class "flex text-sm font-semibold text-ol-gray dark:text-white"}
      (icon/briefcase {:class "h-6 w-6 flex-none text-stone-400 dark:text-stone-500"})
      [:span {:class "ml-3"} "Work"]]
     [:ol {:class "mt-6 space-y-4"}
      (for [role-data resume-data]
        (role role-data))]
     #_(button/button {:href    "#"
                       :variant "secondary"
                       :class   "group mt-6 w-full"}
                      "Download CV"
                      (arrow-down-icon {:class "h-4 w-4 stroke-ol-light-gray transition group-active:stroke-ol-gray dark:group-hover:stroke-white dark:group-active:stroke-white"}))]))

(defn photos
  "Photos component displaying a rotating gallery"
  []
  (let [rotations ["rotate-2" "-rotate-2" "rotate-2" "rotate-2" "-rotate-2"]
        images    [{:src "/images/photos/image-3.png"}
                   {:src "/images/photos/bikeraft1.jpg" :class "object-left"}
                   {:src "/images/photos/ol3.png"}
                   {:src "/images/photos/image-2.png" :class "object-top"}
                   {:src "/images/photos/bikeraft2.jpg"}]]
    [:div {:class "mt-16 sm:mt-20"}
     [:div {:class "-my-4 flex justify-center gap-5 overflow-hidden py-4 sm:gap-8"}
      (for [[idx {:keys [src class]}] (map-indexed vector images)
            :let                      [rotation (get rotations (mod idx (count rotations)))]]
        [:div {:key   src
               :class (str "relative aspect-9/10 w-44 flex-none overflow-hidden rounded-xl bg-stone-100 sm:w-72 sm:rounded-2xl dark:bg-stone-800 " rotation)}
         [:img {:src   src
                :alt   ""
                :sizes "(min-width: 1024px) 42rem, (min-width: 640px) 18rem, 11rem"
                :class (str (or class "") " absolute inset-0 h-full w-full object-cover")}]])]]))

(defn background-image
  "Background image component"
  []
  [:div {:class "mx-auto mt-16 overflow-hidden sm:mt-20"}
   [:div {:class "relative h-[300px] w-full"}
    [:img {:src   "/assets/outskirts-bg-1600x300.png"
           :alt   "Outskirts Labs Background"
           :class "absolute inset-0 h-full w-full object-cover"}]]])

(defn home
  "Main home page component"
  [{:keys [articles]}]
  (list
   (container/container {:class "mt-18"}
                        [:div {:class "max-w-2xl"}
                         [:h1 {:class "text-4xl font-bold tracking-tight text-stone-800 sm:text-5xl dark:text-stone-100"}
                          "Casey Link" [:span {:class "text-xl ml-2 text-stone-500 dark:text-stone-400"} "@Ramblurr"]]
                         [:p {:class "mt-6 text-base text-stone-800 dark:text-stone-100"}
                          "I'm Casey Link, Principal at "
                          [:a {:href  "https://outskirtslabs.com"
                               :class "text-ol-orange-600 transition-colors rounded hover:bg-stone-100 dark:hover:bg-stone-900/50"}
                           "Outskirts Labs"]
                          ", specializing in custom design and software engineering for NGOs and social enterprises. I create technical solutions that make a positive impact while solving complex challenges."]

                         [:div {:class "mt-6 flex gap-6"}
                          (social-link {:href       "https://github.com/Ramblurr"
                                        :aria-label "Follow on GitHub"
                                        :icon       icon/github})
                          #_(social-link {:href     "https://twitter.com/ramblurr"
                                          :icon     icon/the-social-network-formerly-known-as-twitter-fill
                                          :children "Follow on X"})
                          (social-link {:href     "https://bsky.app/profile/casey.link"
                                        :icon     icon/bluesky-outline
                                        :children "Follow on Bluesky"})]])
   (photos)
   (container/container {:class "mt-24 md:mt-28"}
                        [:div {:class "mx-auto grid max-w-xl grid-cols-1 gap-y-20 lg:max-w-none lg:grid-cols-2"}
                         [:div {:class "flex flex-col gap-16"}
                          (for [article-data (take 4 articles)]
                            (article article-data))]
                         [:div {:class "space-y-10 lg:pl-16 xl:pl-24"}
                          #_(newsletter)
                          (resume)]])))

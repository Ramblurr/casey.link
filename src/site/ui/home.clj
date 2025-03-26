(ns site.ui.home
  (:require
   [site.ui.container :as container]
   [site.ui.home.card :as card]
   [site.ui.home.button :as button]
   [site.ui.home.social-icons :as social-icons])
  (:import (java.time Year)))

;; SVG Icon Components
(defn mail-icon
  "Mail icon SVG component"
  [{:keys [class]}]
  [:svg {:viewBox         "0 0 24 24"
         :fill            "none"
         :stroke-width    "1.5"
         :stroke-linecap  "round"
         :stroke-linejoin "round"
         :aria-hidden     "true"
         :class           class}
   [:path {:d     "M2.75 7.75a3 3 0 0 1 3-3h12.5a3 3 0 0 1 3 3v8.5a3 3 0 0 1-3 3H5.75a3 3 0 0 1-3-3v-8.5Z"
           :class "fill-white/80 stroke-ol-light-gray dark:fill-white/5 dark:stroke-ol-light-gray"}]
   [:path {:d     "m4 6 6.024 5.479a2.915 2.915 0 0 0 3.952 0L20 6"
           :class "stroke-ol-light-gray dark:stroke-ol-light-gray"}]])

(defn briefcase-icon
  "Briefcase icon SVG component"
  [{:keys [class]}]
  [:svg {:viewBox "0 0 24 24"
         :fill "none"
         :stroke-width "1.5"
         :stroke-linecap "round"
         :stroke-linejoin "round"
         :aria-hidden "true"
         :class class}
   [:path {:d "M2.75 9.75a3 3 0 0 1 3-3h12.5a3 3 0 0 1 3 3v8.5a3 3 0 0 1-3 3H5.75a3 3 0 0 1-3-3v-8.5Z"
           :class "fill-white/80 stroke-ol-light-gray dark:fill-white/5 dark:stroke-ol-light-gray"}]
   [:path {:d "M3 14.25h6.249c.484 0 .952-.002 1.316.319l.777.682a.996.996 0 0 0 1.316 0l.777-.682c.364-.32.832-.319 1.316-.319H21M8.75 6.5V4.75a2 2 0 0 1 2-2h2.5a2 2 0 0 1 2 2V6.5"
           :class "stroke-ol-light-gray dark:stroke-ol-light-gray"}]])

(defn arrow-down-icon
  "Arrow down icon SVG component"
  [{:keys [class]}]
  [:svg {:viewBox "0 0 16 16"
         :fill "none"
         :aria-hidden "true"
         :class class}
   [:path {:d "M4.75 8.75 8 12.25m0 0 3.25-3.5M8 12.25v-8.5"
           :stroke-width "1.5"
           :stroke-linecap "round"
           :stroke-linejoin "round"}]])

;; Main Component Functions
(defn article
  "Article component for displaying a single article"
  [{:keys [title slug date description]}]
  (card/card {:as :article}
             (card/title {:-href (str "/articles/" slug)} title)
             (card/eyebrow {:-as :time :-datetime date :-decorate? true} date)
             (card/description description)
             (card/cta "Read article")))

(defn social-link
  "Social link component for a single social media link"
  [{:keys [href aria-label icon]}]
  [:a {:href       href
       :aria-label aria-label
       :class      "group -m-1 p-1"}
   (icon {:class "h-6 w-6 fill-ol-light-gray transition group-hover:fill-ol-orange dark:fill-ol-light-gray dark:group-hover:fill-ol-orange"})])

(defn newsletter
  "Newsletter signup component"
  []
  [:form {:action "/thank-you"
          :class  "rounded-2xl border border-ol-light-gray/10 p-6 dark:border-ol-light-gray/20"}
   [:h2 {:class "flex text-sm font-semibold text-ol-gray dark:text-white"}
    (mail-icon {:class "h-6 w-6 flex-none"})
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
  [{:keys [company title logo start end]}]
  (let [start-label (if (string? start) start (:label start))
        start-date  (if (string? start) start (:datetime start))
        end-label   (if (string? end) end (:label end))
        end-date    (if (string? end) end (:datetime end))]
    [:li {:class "flex gap-4"}
     [:div {:class "relative mt-1 flex h-10 w-10 flex-none items-center justify-center rounded-full shadow-md ring-1 shadow-ol-gray/5 ring-ol-gray/5 dark:border dark:border-ol-light-gray/20 dark:bg-ol-gray dark:ring-0"}
      [:img {:src   logo
             :alt   ""
             :class "h-7 w-7"}]]
     [:dl {:class "flex flex-auto flex-wrap gap-x-2"}
      [:dt {:class "sr-only"} "Company"]
      [:dd {:class "w-full flex-none text-sm font-medium text-ol-gray dark:text-white"}
       company]
      [:dt {:class "sr-only"} "Role"]
      [:dd {:class "text-xs text-stone-100 dark:text-stone-100"}
       title]
      [:dt {:class "sr-only"} "Date"]
      [:dd {:class      "ml-auto text-xs text-stone-100 dark:text-stone-100"
            :aria-label (str start-label " until " end-label)}
       [:time {:datetime start-date} start-label]
       " — "
       [:time {:datetime end-date} end-label]]]]))

(defn resume
  "Resume component showing work experience"
  []
  (let [current-year (.getValue (Year/now))
        resume-data  [{:company "Outskirts Labs"
                       :title   "Founder & Lead Developer"
                       :logo    "/images/logos/planetaria.svg"
                       :start   "2019"
                       :end     {:label    "Present"
                                 :datetime (str current-year)}}
                      {:company "Airbnb"
                       :title   "Lead Technical Architect"
                       :logo    "/images/logos/airbnb.svg"
                       :start   "2015"
                       :end     "2019"}
                      {:company "Cropster GmbH"
                       :title   "Software Engineer"
                       :logo    "/images/logos/facebook.svg"
                       :start   "2012"
                       :end     "2015"}
                      {:company "KDE Community"
                       :title   "Open Source Contributor"
                       :logo    "/images/logos/starbucks.svg"
                       :start   "2008"
                       :end     {:label    "Present"
                                 :datetime (str current-year)}}]]
    [:div {:class "rounded-2xl border border-ol-light-gray/10 p-6 dark:border-ol-light-gray/20"}
     [:h2 {:class "flex text-sm font-semibold text-ol-gray dark:text-white"}
      (briefcase-icon {:class "h-6 w-6 flex-none"})
      [:span {:class "ml-3"} "Experience"]]
     [:ol {:class "mt-6 space-y-4"}
      (for [role-data resume-data]
        (role role-data))]
     (button/button {:href    "#"
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
   (container/container {:class "mt-9"}
                        [:div {:class "max-w-2xl"}
                         [:h1 {:class "text-4xl font-bold tracking-tight text-stone-800 sm:text-5xl dark:text-stone-100"}
                          "Developer, Technical Strategist, and NGO Specialist"]
                         [:p {:class "mt-6 text-base text-stone-800 dark:text-stone-100"}
                          "I'm Casey Link, founder of Outskirts Labs, specializing in software development and data engineering for NGOs and non-profit organizations. I create technical solutions that make a positive impact while solving complex challenges."]
                         [:div {:class "mt-6 flex gap-6"}
                          (social-link {:href       "https://github.com/Ramblurr"
                                        :aria-label "Follow on GitHub"
                                        :icon       social-icons/github-icon})
                          (social-link {:href       "https://linkedin.com/in/kaseylink"
                                        :aria-label "Follow on LinkedIn"
                                        :icon       social-icons/linkedin-icon})]])
   (photos)
   (container/container {:class "mt-24 md:mt-28"}
                        [:div {:class "mx-auto grid max-w-xl grid-cols-1 gap-y-20 lg:max-w-none lg:grid-cols-2"}
                         [:div {:class "flex flex-col gap-16"}
                          (for [article-data (take 4 articles)]
                            (article article-data))]
                         [:div {:class "space-y-10 lg:pl-16 xl:pl-24"}
                          (newsletter)
                          (resume)]])))

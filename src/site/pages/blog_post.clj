(ns site.pages.blog-post
  (:require
   [site.pages.render :as render]
   [clojure.string :as str]
   [site.dev :as dev]
   [site.markdown :as md]
   [site.ui :as ui]
   [site.ui.alert :as alert]
   [site.ui.prose :as prose])
  (:import
   (java.net URLEncoder)
   (java.time LocalDate ZoneOffset)
   (java.time.format DateTimeFormatter)))

(def outdated-alert
  [alert/Alert {::alert/title "This post is over 5 years old."
                ::alert/type  :warning}
   [:p "Information may be outdated or no longer relevant."]])

(defn parse-date
  ^LocalDate [date-str]
  (when parse-date
    (LocalDate/parse date-str)))

(defn maybe-outdated
  "Inject an alert if the post is outdated.
   A post is considered outdated if it has a date more than 5 years in the past and does not have the 'evergreen' tag."
  [{:keys [title tags date]} content]
  (if (and (not (contains? tags "evergreen"))
           (.isAfter (LocalDate/now) (.plusYears (parse-date ^LocalDate date) 5)))
    (into (list outdated-alert content))
    content))

(defn format-date
  "Format date in US format (e.g., January 12, 2023)"
  [date-str]
  (if (and date-str (not (str/blank? date-str)))
    (try
      (when-let [date (parse-date date-str)]
        (.format ^LocalDate date (DateTimeFormatter/ofPattern "MMMM d, yyyy")))
      (catch Exception _
        "No date available"))
    "No date available"))

(defn- url-encode ^String [^String s]
  (URLEncoder/encode s "UTF-8"))

(defn- mailto-url [post-url]
  (str "mailto:casey@outskirtslabs.com?subject=" (url-encode (str "Re: " post-url))))

(defn- bluesky-intent-url [post-url]
  (str "https://bsky.app/intent/compose?text=" (url-encode (str "@casey.link " post-url))))

(def ^:private reply-link-class
  "text-zinc-600 underline decoration-zinc-300 underline-offset-2 transition any-hover:text-ol-orange-500 any-hover:decoration-ol-orange-500 dark:text-zinc-400 dark:decoration-zinc-600 dark:any-hover:text-ol-orange-400 dark:any-hover:decoration-ol-orange-400")

(defn- reply-section [base-url uri]
  (let [post-url (str base-url uri)]
    [:footer {:class "mt-16 pt-8 border-t border-zinc-100 dark:border-zinc-700/40"}
     [:p {:class "text-sm text-zinc-500 dark:text-zinc-400"}
      "Reply via: "
      [:a {:href (mailto-url post-url) :class reply-link-class} "Email"]
      " · "
      [:a {:href (bluesky-intent-url post-url) :target "_blank" :class reply-link-class} "Bluesky"]]]))

(defn render [req {:page/keys [title uri resource-path body] :blog/keys [date] :as page}]
  (render/with-body page
    (ui/main
     [:div {:class "sm:px-8 mt-16 lg:mt-32"}
      [:section {:id "article-container" :class ""}
       [:article {:class "max-w-2xl lg:max-w-5xl"}
        [:header {:class "flex flex-col"}
         [:h1 {:class "mt-6 text-4xl font-bold tracking-tight text-zinc-800 sm:text-5xl dark:text-zinc-100"}
          title]
         [:time {:datetime date
                 :class    "flex items-center text-base text-zinc-500 dark:text-zinc-400"}
          [:span {:class "h-4 w-0.5 rounded-full bg-zinc-400 dark:bg-zinc-500"}]
          [:span {:class "ml-3"} (format-date date)]]]
        (prose/prose {:class "mt-8"}
                     (md/->hiccup body))
        (reply-section (:base-url req) uri)]]])))

(defmethod render/page-content :page.kind/blog-post
  [{:blog/keys [tags modified date] :page/keys [title description uri] :as page} req]
  (-> (render req page)
      (assoc
       :open-graph/type "article"
       :ld-json/value {"@context"      "http://schema.org"
                       "@type"         "BlogPosting"
                       "headline"      title
                       "author"        {"@type" "Person"
                                        "name"  "Casey Link"
                                        "url"   "https://casey.link"}
                       "datePublished" date
                       "dateCreated"   date
                       "url"           (str (:base-url req) uri)
                       "dateModified"  modified
                       "description"   description
                       "inLanguage"    "en-US"
                       "keywords"      (str/join ", " tags)}
       :page/head (list
                   [:link {:rel   "alternate"
                           :type  "application/atom+xml"
                           :title "Casey Link's Weblog"
                           :href  "/atom/articles"}]
                   [:meta {:property "article:author" :content (:base-url req)}]
                   [:meta {:property "article:published_time" :content date}]
                   (when modified
                     [:meta {:property "article:modified_time" :content modified}])
                   (for [tag tags]
                     [:meta {:property "article:tag" :content tag}])))))

(dev/re-render!)

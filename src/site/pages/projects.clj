(ns site.pages.projects
  (:require
   [site.ui :as ui]
   [clojure.string :as str]
   [site.dev :as dev]
   [site.pages.render :as render]
   [site.ui.home.card :as card]
   [site.ui.icons :as icon]
   [site.ui.simple-layout :as simple-layout]))

(defn project-card [{:keys [name description href label icon tags]}]
  (card/card {:as :li}
             #_[:div {:class "relative z-10 flex h-12 w-12 items-center justify-center rounded-full bg-white ring-1 shadow-md shadow-stone-800/5 ring-stone-900/5 dark:border dark:border-stone-700/50 dark:bg-stone-800 dark:ring-0"}
                [:div
                 {:class
                  "relative z-10 flex h-12 w-12 items-center justify-center rounded-full bg-white shadow-md ring-1 shadow-stone-800/5 ring-stone-900/5 dark:border dark:border-stone-700/50 dark:bg-stone-800 dark:ring-0"}
                 [:img
                  {:alt       "",
                   :width     "32",
                   :src       "https://spotlight.tailwindui.com/_next/static/media/planetaria.ecd81ade.svg"
                   :style     {:color "transparent"},
                   :loading   "lazy",
                   :class     "h-8 w-8",
                   :data-nimg "1",
                   :decoding  "async",
                   :height    "32"}]]]
             #_[:h2 {:class "mt-6 text-base font-semibold text-stone-800 dark:text-stone-100"}
                [:a {:href (:link project) :target "_blank"}
                 (:name project)]]
             (card/title {:class "mt-6" :-href href :rel "noopener noreferrer"} name)
             (card/description description)
             [:p {:class "relative z-10 mt-6 flex text-sm font-medium text-stone-400 transition group-any-hover:text-ol-orange-500 dark:text-stone-200"}
              ((or icon icon/link) {:class "h-5 w-5 flex-none"})
              [:span {:class "ml-2"}
               label]]
             (when (seq tags)
               [:div {:class "relative flex text-sm text-stone-400 transition group-any-hover:text-ol-orange-500 dark:text-stone-200"}
                (icon/stack {:class      "h-5 w-5 flex-none"
                             :title      "Tech Stack"
                             :aria-label "Tech Stack"
                             :role       "img"})
                [:span {:class "ml-2"}
                 (str/join ", " tags)]])))

(def all-projects [{:name        "Probematic",
                    :description "A tool to help an anarchist band manage itself. Built with Clojure and HTMX."
                    :href        "https://github.com/Ramblurr/probematic"
                    :label       "Ramblurr/probematic"
                    :icon        icon/github
                    :tags        ["clojure" "htmx"]}
                   {:name        "wayland-java"
                    :description "Modern (JDK 22+) Java/JVM bindings for libwayland and wayland-protocols"
                    :href        "https://github.com/Ramblurr/wayland-java"
                    :label       "Ramblurr/wayland-java"
                    :icon        icon/github
                    :tags        ["java" "wayland"]}
                   {:name        "datastar-expressions"
                    :description "A proof-of-concept for writing 🚀 datastar expressions using Clojure without manual string concatenation."
                    :href        "https://github.com/Ramblurr/datastar-expressions"
                    :label       "Ramblurr/datastar-expressions"
                    :icon        icon/github
                    :tags        ["javascript"]}
                   {:name        "datomic-pro-flake"
                    :description "A Nix flake providing a Datomic Pro package and NixOS modules"
                    :href        "https://github.com/Ramblurr/datomic-pro-flake"
                    :label       "Ramblurr/datomic-pro-flake"
                    :icon        icon/github
                    :tags        ["nix" "datomic" "clojure"]}
                   {:name        "nixcfg"
                    :description "My nix flake ❄️ for all my servers, workstations, pis, etc."
                    :href        "https://github.com/Ramblurr/nixcfg"
                    :label       "Ramblurr/nixcfg"
                    :icon        icon/github
                    :tags        ["nix"]}
                   {:name        "My Blog"
                    :description "A blog about code, open-source, security, and tech in the social sector."
                    :href        "https://casey.link/articles"
                    :label       "https://casey.link/articles"
                    :icon        icon/link
                    :tags        ["english"]}
                   {:name        "hifi-crud"
                    :description "A small clojure framework for builing immediate-mode web applications with Datastar"
                    :href        "Ramblurr/hifi-crud"
                    :label       "github.com/Ramblurr/hifi-crud"
                    :icon        icon/rocket
                    :tags        ["clojure" "datastar"]}
                   {:name        "datahike-sqlite"
                    :description "Datahike sqlite data storage backend"
                    :archived?   true
                    :href        "https://github.com/Ramblurr/datahike-sqlite"
                    :label       "Ramblurr/datahike-sqlite"
                    :tags        ["clojure"]}
                   {:name        "fairybox"
                    :description "Screenless RFID Raspberry PI audio player for children"
                    :href        "https://github.com/Ramblurr/fairybox"
                    :label       "Ramblurr/fairybox"
                    :icon        icon/github
                    :tags        ["hardware" "clojure" "raspberry-pi" "maker"]}
                   {:name        "AnkiDroid"
                    :description "Anki flashcards on Android. I started this project back in 2009, and handed it off to the community sometime later."
                    :href        "https://github.com/ankidroid/Anki-Android"
                    :label       "ankidroid/Anki-Android"
                    :icon        icon/github
                    :tags        ["android" "java"]
                    :archived?   true}
                   {:name        "Quechua Notes"
                    :description "A collection of notes and resources for the Quechua language, from my time studying the language in 2011-2012."
                    :href        "https://quechua.binaryelysium.com/"
                    :label       "quechua.binaryelysium.com"
                    :tags        ["language-learning"]
                    :archived?   true}
                   {:name        "Piet Creator"
                    :description "An IDE for developing and debugging programs written in the Piet esolang"
                    :href        "https://github.com/Ramblurr/PietCreator"
                    :label       "Ramblurr/PietCreator"
                    :tags        ["c++" "qt"]
                    :icon        icon/github}
                   {:name        "MP3Tunes Android"
                    :description "My first Android app (for a now defunct music platform) released just several months after the first Android phone. *sigh* What crazy days those were."
                    :href        "https://github.com/Ramblurr/mp3tunes-android"
                    :label       "Ramblurr/mp3tunes-android"
                    :icon        icon/github
                    :tags        ["android" "java"]
                    :archived?   true}])

(def active (remove #(:archived? %) all-projects))
(def archived (filter #(:archived? %) all-projects))

(defn render [_req page]
  (let [description (format
                     "Over %d years in the tech industry tends to produce quite the collection of personal experiments, side projects, and creative ventures—some still actively evolving, others now resting in the archives."
                     (- (.getValue (java.time.Year/now)) 2008))]
    (->
     (render/with-body page
       (ui/main
        (simple-layout/simple-layout
         {:title    "Personal Projects"
          :intro    (list [:p description]
                          [:p {:class "mt-4"} "Below you will find a selection that reflects the range of my technical interests and skillset."])
          :children [:div
                     [:ul {:class "grid grid-cols-1 gap-x-12 gap-y-16 sm:grid-cols-2 lg:grid-cols-3" :role "list"}
                      (for [project active]
                        (project-card project))]

                     [:h1 {:class "mt-20 text-2xl font-bold tracking-tight text-stone-800 sm:text-3xl dark:text-stone-100"}
                      "Archived Projects"]
                     [:p {:class "mt-6 text-base text-stone-600 dark:text-stone-400"}
                      "These projects are no longer actively maintained, but they may still be of interest."]

                     [:ul {:class "mt-16 grid grid-cols-1 gap-x-12 gap-y-16 sm:grid-cols-2 lg:grid-cols-3" :role "list"}
                      (for [project archived]
                        (project-card project))]]})))
     (assoc :page/description description))))

(comment
  (projects nil))

(defmethod render/page-content :page.kind/project-index
  [page req]
  (render req page))

(dev/re-render!)

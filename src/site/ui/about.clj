(ns site.ui.about
  (:require [site.ui.container :as container]
            [site.ui.home.social-icons :refer [github-icon instagram-icon linkedin-icon x-icon]]
            [site.ui.about.mail-icon :refer [mail-icon]]
            [site.ui.about.social-link :refer [social-link]]))

(defn about []
  (container/container  {:class "mt-16 sm:mt-32"}
                        [:div {:class "grid grid-cols-1 gap-y-16 lg:grid-cols-2 lg:grid-rows-[auto_1fr] lg:gap-y-12"}
                         [:div {:class "lg:pl-20"}
                          [:div {:class "max-w-xs px-2.5 lg:max-w-none"}
                           [:img {:src   "/public/images/portrait.jpg"
                                  :alt   ""
                                  :class "aspect-square rotate-3 rounded-2xl bg-zinc-100 object-cover dark:bg-zinc-800"}]]]
                         [:div {:class "lg:order-first lg:row-span-2"}
                          [:h1 {:class "text-4xl font-bold tracking-tight text-zinc-800 sm:text-5xl dark:text-zinc-100"}
                           "I'm Casey Link. I live in Vienna, Austria, where I design the future."]
                          [:div {:class "mt-6 space-y-7 text-base text-zinc-600 dark:text-zinc-400"}
                           [:p "I've loved making things for as long as I can remember, and wrote my first program when I was 6 years old."]
                           [:p "The only thing I loved more than computers as a kid was space."]
                           [:p "Today, I work on a variety of projects focusing on data privacy, security, and user experience."]]]
                         [:div {:class "lg:pl-20"}
                          [:ul {:role "list"}
                           (social-link {:href     "https://twitter.com/ramblurr"
                                         :icon     x-icon
                                         :children "Follow on X"})
                           (social-link {:href       "https://instagram.com/ramblurr"
                                         :icon       instagram-icon
                                         :class-name "mt-4"
                                         :children   "Follow on Instagram"})
                           (social-link {:href       "https://github.com/ramblurr"
                                         :icon       github-icon
                                         :class-name "mt-4"
                                         :children   "Follow on GitHub"})
                           (social-link {:href       "https://linkedin.com/in/ramblurr"
                                         :icon       linkedin-icon
                                         :class-name "mt-4"
                                         :children   "Follow on LinkedIn"})
                           (social-link {:href       "mailto:casey@caseylink.com"
                                         :icon       mail-icon
                                         :class-name "mt-8 border-t border-zinc-100 pt-8 dark:border-zinc-700/40"
                                         :children   "casey@caseylink.com"})]]]))

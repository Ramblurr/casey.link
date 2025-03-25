(ns site.ui.home.button)

(def variant-styles
  {:primary "bg-ol-orange font-semibold text-white hover:bg-dark-orange active:bg-ol-brown active:text-white/90 dark:bg-ol-orange-dark dark:hover:bg-dark-orange-dark dark:active:bg-ol-brown-dark dark:active:text-white/90"
   :secondary "bg-white font-medium text-ol-gray hover:bg-ol-light-gray/10 active:bg-ol-light-gray/20 active:text-ol-gray/80 dark:bg-ol-gray-dark/60 dark:text-white dark:hover:bg-ol-gray-dark/80 dark:hover:text-white dark:active:bg-ol-gray-dark/60 dark:active:text-white/80"
   :turquoise "bg-turquoise font-semibold text-white hover:bg-turquoise/90 active:bg-turquoise/80 active:text-white/90 dark:bg-turquoise-dark dark:hover:bg-turquoise-dark/90 dark:active:bg-turquoise-dark/80 dark:active:text-white/90"})

(defn button
  "Button component that can be a link or a button"
  [{:keys [href type variant class]} & children]
  (let [variant (or variant :primary)
        class (str
               "inline-flex items-center gap-2 justify-center rounded-md py-2 px-3 text-sm outline-offset-2 transition active:transition-none "
               (get variant-styles variant)
               " "
               class)]
    (if href
      [:a {:href href :class class} children]
      [:button {:type (or type "button") :class class} children])))
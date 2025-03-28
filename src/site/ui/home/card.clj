(ns site.ui.home.card
  (:require [site.ui.icons :as icon]
            [site.ui.core :as uic]))

(defn card
  "Card component"
  [{:keys [as class]} & children]
  (let [element     (or as :div)
        element-str (if (keyword? element) (name element) element)]
    [(keyword element-str) {:class (str "group relative flex flex-col items-start " class)}
     children]))

(defn- card-link
  "Link wrapper for card content"
  [{:keys [href]} & children]
  (list
   [:div {:class
          "absolute -inset-x-4 -inset-y-6 z-0 scale-95 bg-stone-100 opacity-0 transition group-hover:scale-100 group-hover:opacity-100 sm:-inset-x-6 sm:rounded-2xl dark:bg-stone-900/50"}]
   [:a {:href href}
    [:span {:class "absolute -inset-x-4 -inset-y-6 z-20 sm:-inset-x-6 sm:rounded-2xl"}]
    [:span {:class "relative z-10"} children]]))

(defn title
  "Title component for card"
  [& args]
  (let [[opts attrs children] (uic/extract args)
        {:keys [as href]
         :or   {as :h2}}      opts]
    [as {:class "text-base font-semibold tracking-tight text-stone-800 dark:text-stone-100"}
     (if href
       (card-link {:href href} children)
       children)]))

(defn description
  "Description component for card"
  [& children]
  [:p {:class "relative z-10 mt-2 text-sm text-stone-600 dark:text-stone-400"}
   children])

(defn cta
  "Call to action component for card"
  [& children]
  [:div {:aria-hidden "true"
         :class       "relative z-10 mt-4 flex items-center text-sm font-medium text-ol-orange dark:text-ol-orange"}
   children
   (icon/chevron-right {:class "ml-1 h-4 w-4"})])

(defn eyebrow
  "Eyebrow component for card - like a subtitle or date"
  [& args]
  (let [[opts attrs children] (uic/extract args)
        {:keys [as decorate? datetime]
         :or   {as :p}}       opts]
    [as
     (uic/merge-attrs attrs
                      :class
                      (uic/cs "relative z-10 order-first mb-3 flex items-center text-sm text-zinc-400 dark:text-zinc-500" (when decorate? "pl-3.5"))
                      :datetime datetime)
     (when decorate?
       [:span {:class       "absolute inset-y-0 left-0 flex items-center"
               :aria-hidden "true"}
        [:span {:class "h-4 w-0.5 rounded-full bg-ol-light-gray/30 dark:bg-ol-light-gray/50"}]])
     children]))

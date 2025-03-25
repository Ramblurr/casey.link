(ns site.ui.home.card)

(defn chevron-right-icon
  "Chevron right icon SVG component"
  [{:keys [class]}]
  [:svg {:viewBox "0 0 16 16"
         :fill "none"
         :aria-hidden "true"
         :class class}
   [:path {:d "M6.75 5.75 9.25 8l-2.5 2.25"
           :stroke-width "1.5"
           :stroke-linecap "round"
           :stroke-linejoin "round"}]])

(defn card
  "Card component"
  [{:keys [as class]} & children]
  (let [element (or as :div)
        element-str (if (keyword? element) (name element) element)]
    [(keyword element-str) {:class (str "group relative flex flex-col items-start " class)}
     children]))

(defn card-link
  "Link wrapper for card content"
  [{:keys [href]} & children]
  (list
   [:div {:class "absolute -inset-x-4 -inset-y-6 z-0 scale-95 bg-ol-light-gray/5 opacity-0 transition group-hover:scale-100 group-hover:opacity-100 sm:-inset-x-6 sm:rounded-2xl dark:bg-ol-gray/50"}]
   [:a {:href href}
    [:span {:class "absolute -inset-x-4 -inset-y-6 z-20 sm:-inset-x-6 sm:rounded-2xl"}]
    [:span {:class "relative z-10"} children]]))

(defn title
  "Title component for card"
  [{:keys [as href]} & children]
  (let [element (or as :h2)
        element-str (if (keyword? element) (name element) element)]
    [(keyword element-str) {:class "text-base font-semibold tracking-tight text-ol-gray dark:text-white"}
     (if href
       (card-link {:href href} children)
       children)]))

(defn description
  "Description component for card"
  [& children]
  [:p {:class "relative z-10 mt-2 text-sm text-dark-liver dark:text-ol-light-gray"}
   children])

(defn cta
  "Call to action component for card"
  [& children]
  [:div {:aria-hidden "true"
         :class "relative z-10 mt-4 flex items-center text-sm font-medium text-ol-orange dark:text-ol-orange"}
   children
   (chevron-right-icon {:class "ml-1 h-4 w-4 stroke-current"})])

(defn eyebrow
  "Eyebrow component for card - like a subtitle or date"
  [{:keys [as decorate class datetime]} & children]
  (let [element (or as :p)
        element-str (if (keyword? element) (name element) element)
        decorate-class (if decorate "pl-3.5" "")]
    [(keyword element-str) {:class (str "relative z-10 order-first mb-3 flex items-center text-sm text-ol-light-gray dark:text-ol-light-gray " decorate-class " " class)
              :datetime datetime}
     (when decorate
       [:span {:class "absolute inset-y-0 left-0 flex items-center"
               :aria-hidden "true"}
        [:span {:class "h-4 w-0.5 rounded-full bg-ol-light-gray/30 dark:bg-ol-light-gray/50"}]])
     children]))
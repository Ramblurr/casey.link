(ns site.ui
  (:require
   [site.html :as html]
   [site.ui.footer :as footer]
   [site.ui.header :as header]))

(defn main [& children]
  [:main {:id "main" :class "flex-auto"} children])

(defn layout [{:page/keys [uri body]}]
  [:div {:class "flex w-full"}
   [:div {:class "fixed inset-0 flex justify-center sm:px-8"}
    [:div {:class "flex w-full max-w-7xl lg:px-8"}
     [:div {:class "w-full bg-white ring-1 ring-zinc-100 dark:bg-stone-800 dark:ring-zinc-300/20"}]]]
   [:div {:class "relative flex w-full flex-col"}
    (header/header {:path uri})
    body
    (footer/footer)]])

(defn shell [{:page/keys [description uri title head] :as page}]
  (assoc page :content
         [html/doctype-html5
          [:html (array-map :lang                  "en"
                            :class "h-full antialiased"
                            :data-signals-darkmode "window.matchMedia(\"(prefers-color-scheme: dark)\").matches"
                            :data-persist          "darkmode"
                            :data-class-dark       "$darkmode")
           [:head
            [:meta {:http-equiv "content-type" :content "text/html;charset=UTF-8"}]
            [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
            (when description
              [:meta {:property "description" :content description}])
            (when-let [description (or (:open-graph/description page) description)]
              [:meta {:property "og:description" :content description}])
            (when title
              [:title title])
            (when-let [title (or (:open-graph/title page) title)]
              [:meta {:property "og:title" :content title}])
            [:meta {:property "og:type" :content (or (:open-graph/type page) "website")}]
            #_[:script {:type "importmap"}
               (html/raw
                (json/write-value-as-string {:imports {"datastar" "/js/datastar@1.0.0-RC.11.js"}}))]
            [:script {:defer true :type "module" :src "/js/datastar@1.0.0-RC.11.js"}]
            [:script {:defer true :src "/js/prism.js"}]
            [:link {:href "/site.css" :rel "stylesheet" :type "text/css"}]
            head]
           [:body {:class "flex h-full bg-stone-200 dark:bg-stone-900"}
            [:div {:data-on-load (format "@post('/dev?uri=%s')" uri)}]
            (layout page)
            [:script {:defer true :src "/js/header.js"}]
            [:script {:defer true :type "module" :src "/js/flask.js"}]]]]))

(ns site.sitemap
  (:require
   [dev.onionpancakes.chassis.core :as h]
   [reitit.core :as reitit]))

(defn- generate-url-entry [entry]
  [:url
   [:loc (:loc entry)]
   (when-let [x (:lastmod entry)]
     [:lastmod x])
   (when-let [x (:changefreq entry)]
     [:changefreq x])
   (when-let [x (:priority entry)]
     [:priority x])])

(defn- generate-url-entries [entries]
  (h/html
   [:urlset {:xmlns "http://www.sitemaps.org/schemas/sitemap/0.9"}
    (map generate-url-entry entries)]))

(defn generate-sitemap
  "Render Clojure data structures to a String of sitemap XML."
  [url-entries]
  (str
   (h/html
    (h/raw "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"))
   (generate-url-entries url-entries)))

(defn route->sitemap-entry
  [public-url [url {:sitemap/keys [last-modified exclude?] :as data}]]
  (when-not exclude?
    {:loc     (str public-url url)
     :lastmod last-modified}))

(defn create-sitemap-handler [{:keys [base-url]}]
  (fn sitemap-handler [req]
    (let [r
          (map #(route->sitemap-entry base-url %)
               (-> req :reitit.core/router reitit/routes))]
      (prn r)
      {:status  200
       :headers {"content-type" "text/xml"}
       :body    (generate-sitemap r)})))

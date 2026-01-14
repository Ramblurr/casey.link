(ns site.feed
  "Atom 1.0 feed generation for blog articles."
  (:require
   [clojure.string :as str]
   [dev.onionpancakes.chassis.core :as h]
   [site.db :as db]
   [site.html :as html]
   [site.markdown :as md])
  (:import
   (java.time LocalDate ZoneOffset)
   (java.time.format DateTimeFormatter)))

(def ^:private feed-limit 15)

(def ^:private feed-title "Casey Link's Weblog")

(def ^:private feed-author "Casey Link")

(defn date-str->rfc3339
  "Convert date string YYYY-MM-DD to RFC 3339 format."
  [date-str]
  (let [local-date (LocalDate/parse date-str)
        zdt        (.atStartOfDay local-date ZoneOffset/UTC)]
    (.format zdt DateTimeFormatter/ISO_INSTANT)))

(defn post-url
  "Generate absolute URL for a blog post."
  [base-url post]
  (str base-url (:page/uri post)))

(defn extract-domain
  "Extract domain from base URL."
  [base-url]
  (-> base-url
      (str/replace #"^https?://" "")
      (str/replace #"/.*$" "")))

(defn feed-id
  "Generate tag URI for the feed."
  [base-url]
  (str "tag:" (extract-domain base-url) ",2022:/atom/articles"))

(defn entry-id
  "Generate tag URI for a feed entry."
  [base-url post]
  (let [domain (extract-domain base-url)
        date   (:blog/date post)
        uri    (:page/uri post)]
    (str "tag:" domain "," date ":" uri)))

(defn render-content-html
  "Render markdown body to HTML string."
  [body]
  (html/->str (md/->hiccup body)))

(defn author-element
  "Generate Atom author element."
  [name]
  [:author [:name name]])

(defn link-element
  "Generate Atom link element as self-closing XML."
  [href rel type]
  (h/raw (str "<link href=\"" href "\" rel=\"" rel "\" type=\"" type "\" />")))

(defn entry-element
  "Generate Atom entry element for a blog post."
  [base-url post]
  (let [url         (post-url base-url post)
        date        (:blog/date post)
        modified    (or (:blog/modified post) date)
        author      (:blog/author post)
        title       (:page/title post)
        description (:blog/description post)
        body        (:page/body post)
        content     (render-content-html body)]
    [:entry
     [:title title]
     (link-element url "alternate" "text/html")
     [:id (entry-id base-url post)]
     [:published (date-str->rfc3339 date)]
     [:updated (date-str->rfc3339 modified)]
     (author-element author)
     (when description
       [:summary description])
     [:content {:type "html"} (h/raw (str "<![CDATA[" content "]]>"))]]))

(defn feed-updated
  "Get the most recent update date from posts."
  [posts]
  (when (seq posts)
    (let [post (first posts)
          date (or (:blog/modified post) (:blog/date post))]
      (date-str->rfc3339 date))))

(defn feed-element
  "Generate complete Atom feed element."
  [base-url posts]
  [:feed {:xmlns "http://www.w3.org/2005/Atom"}
   [:title feed-title]
   (link-element (str base-url "/atom/articles") "self" "application/atom+xml")
   (link-element base-url "alternate" "text/html")
   [:id (feed-id base-url)]
   [:updated (or (feed-updated posts) (date-str->rfc3339 "2020-01-01"))]
   (author-element feed-author)
   (map #(entry-element base-url %) posts)])

(defn generate-feed
  "Generate complete Atom feed XML string."
  [base-url posts]
  (str
   (h/html (h/raw "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"))
   (h/html (feed-element base-url posts))))

(defn create-feed-handler
  "Create Ring handler for Atom feed.
   Caching (ETag, Cache-Control) handled by wrap-cache middleware."
  [{:keys [base-url]}]
  (fn feed-handler [req]
    (let [db    (:app/db req)
          posts (->> (db/get-blog-posts db)
                     (take feed-limit))
          body  (generate-feed base-url posts)]
      {:status  200
       :headers {"content-type"                "application/xml; charset=utf-8"
                 "access-control-allow-origin" "*"}
       :body    body})))

(ns site.content
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [nextjournal.markdown.utils :as md.util]
   [nextjournal.markdown :as md]
   [nextjournal.markdown.transform :as md.transform]
   [site.ui.icons :as icon])
  (:import (java.time.format DateTimeFormatter)
           (java.time LocalDate ZoneOffset)))

(defn s2sr [s]
  (-> s (java.io.StringReader.) (java.io.BufferedReader.)))

(defn parse-edn-metadata-headers
  [lines-seq]
  (if (re-matches #"\{.*" (or (first lines-seq) ""))
    ;; take sequences until you hit an empty line
    (let [meta-lines (take-while (comp not (partial re-matches #"\s*"))
                                 lines-seq)]
      [(->> meta-lines
            ;; join together and parse
            (str/join "\n")
            edn/read-string)
       ;; count the trailing empty line
       (inc (count meta-lines))])
    [nil 0]))

(defn parse [string]
  (let [s                     (-> string s2sr line-seq)
        [metadata meta-lines] (parse-edn-metadata-headers s)]
    [metadata
     (->> (drop meta-lines s)
          (str/join "\n")
          md/parse
          md.util/insert-sidenote-containers)]))

(defn content-by-type [node type]
  (filter #(= (:type %) type) (:content node)))

(defn lift-block-images
  "Lift an image node to top-level when it is the only child of a paragraph."
  [md-nodes]
  (map (fn [{:as node :keys [type content]}]
         (cond

           (and (= :paragraph type)
                (= 3 (count content))
                (= :image (:type (first content)))
                (= :text (:type (last content))))
           (let [caption-node  (last content)
                 image-node    (first content)
                 alt-text-node (-> image-node :content first)]
             (assoc image-node
                    :content
                    [(assoc alt-text-node :type :alt)
                     (-> caption-node
                         (assoc :type :caption))]))

           (and (= :paragraph type)
                (= 1 (count content))
                (= :image (:type (first content))))
           (first content)

           :else node)) md-nodes))

(defn transform-ast [md-ast]
  (-> md-ast
      (update :content lift-block-images)))

(def transform-ctx
  (assoc md.transform/default-hiccup-renderers
         :image (fn [{:as _ctx ::md.transform/keys [parent]} {:as node :keys [attrs  content]}]
                  ;; This works together with lift-block-images to ensure that "block" images are lifted to the top level.
                  (if (= :doc (:type parent))
                    (let [caption-node  (content-by-type node :caption)
                          alt-text-node (content-by-type node :alt)
                          alt-text      (apply str (map :text alt-text-node))
                          caption-text  (apply str (map :text caption-node))]
                      [:figure.image
                       [:img (assoc attrs :alt alt-text)]
                       (when-not (str/blank? caption-text)
                         [:figcaption.text-center.mt-1 caption-text])])
                    [:img.inline (assoc attrs :alt (md.transform/->text node))]))
         :sidenote-ref (fn [_ {:keys [ref label]}]
                         (let [fn (str (inc ref))]
                           [:a.sidenote-ref {:id (str "fn" fn) :href (str "#fnref" fn) :role "doc-noteref"} [:sup {:data-label label} fn]]))
         :sidenote (fn [ctx {:as node :keys [ref text]}]
                     (let [fn (str (inc ref))]
                       [:span.sidenote {:role "doc-footnote" :id (str "fnref" fn)}
                        [:sup.sidenote-number (str fn ".")]
                        (or text (md.transform/->text node))
                        [:a {:role "doc-backlink" :href (str "#fn" fn) :class "text-inherit"}
                         (icon/arrow-u-up-left {:class "size-4 inline ml-1 text-inherit border-b"})]]))
         :code (fn [ctx {:keys [text info] :as node}]
                 (let [class (when info (str "language-" info))]
                   [:pre [:code {:class class} (or text (md.transform/->text node))]]))
         :plain (partial md.transform/into-markup [:span])))

(defn md->hiccup [string]
  (let [[metadata {:keys [footnotes] :as md-ast}] (parse string)]
    [metadata
     (md.transform/->hiccup transform-ctx (transform-ast md-ast))]))

(comment
  (md->hiccup "_hello_ what and foo[^note1] and
And what.

![](./fairybox2.jpg)

[^note1]: the _what_
")
  (md->hiccup "_hello_ what
And what.

![This is an alt text](./fairybox2.jpg)

")
  ;; => [nil [:div [:p [:em "hello"] " what" " " "And what."] [:p [:img {:src "./fairybox2.jpg", :title nil, :alt ""}]]]]

  ;; => [nil [:div [:p [:em "hello"] " what" " " "And what."] [:p [:img {:src "./fairybox2.jpg", :title nil, :alt ""}]]]]

  (md->hiccup "Wut

Wut
{class=\"foo bar\" id=\"baz\"}


And wut
")
  ;; rcf
  ;;
  )

(defn parse-post [path]
  (let [file               (io/file (str "content/" path "/index.md"))
        [metadata content] (-> file slurp md->hiccup)]
    {:metadata metadata
     :title    (:title metadata)
     :content  content}))

(defn content [page-fn dir req]
  (page-fn
   (parse-post (str dir "/" (-> req :path-params :slug)))))

(defn format-date
  "Format date in US format (e.g., January 12, 2023)"
  [date-str]
  (if (and date-str (not (str/blank? date-str)))
    (try
      (let [date (LocalDate/parse date-str)]
        (.format date (DateTimeFormatter/ofPattern "MMMM d, yyyy")))
      (catch Exception _
        "No date available"))
    "No date available"))

(defn article-dirs []
  (->> (io/file "content/posts")
       (.listFiles)
       (seq)
       (filter #(.isDirectory %))))

(defn get-about []
  (->
   (io/file "content/about.md")
   (slurp)
   (md->hiccup)))

(defn get-articles
  "Get all articles sorted by date (most recent first)"
  []
  (->> (article-dirs)
       (filter #(.exists (io/file (str (.getPath %) "/index.md"))))
       (map (fn [dir]
              (let [slug               (.getName dir)
                    {:keys [metadata]} (parse-post (str "posts/" slug))]
                (assoc metadata
                       :slug slug
                       :date (:date metadata)))))
       (sort-by :date #(compare %2 %1))))

(comment
  (:content
   (parse-post "posts/mobile-security-field-workers"))
  (format-date "2023-01-12")
  (get-articles))

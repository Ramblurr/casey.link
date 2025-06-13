(ns site.markdown
  (:require
   [site.html :as html]
   [site.class-path :as cp]
   [clojure.edn :as edn]

   [site.ui.icons :as icon]
   [clojure.java.io :as io]
   [clojure.string :as str]

   [nextjournal.markdown.utils :as md.util]
   [nextjournal.markdown :as md]
   [nextjournal.markdown.transform :as md.transform]))

(defn parse-edn-frontmatter
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

(defn s2sr [s]
  (-> s (java.io.StringReader.) (java.io.BufferedReader.)))

(defn parse
  "Parse a markdown string with optional EDN frontmatter.
  Returns a tuple of [metadata ast], where `metadata` is a map of frontmatter (if it exists)
  and `ast` is the parsed markdown AST."
  [string]
  (let [s                     (-> string s2sr line-seq)
        [metadata meta-lines] (parse-edn-frontmatter s)]
    [metadata
     (->> (drop meta-lines s)
          (str/join "\n")
          md/parse
          md.util/insert-sidenote-containers)]))

(defn parse-metadata
  "Like `parse`, but returns the metadata and the raw markdown string without parsing it."
  [string]
  (let [s                     (-> string s2sr line-seq)
        [metadata meta-lines] (parse-edn-frontmatter s)]
    [metadata
     (->> (drop meta-lines s)
          (str/join "\n"))]))

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
           (let [image-node (first content)]
             (assoc-in image-node [:content 0 :type] :alt))

           :else node)) md-nodes))

(defn transform-ast [md-ast]
  (-> md-ast
      (update :content lift-block-images)))

(defn embed-hiccup [{:keys [info content] :as node}]
  (try
    (let [data (read-string (get-in content [0 :text]))]
      (when (:embed (meta data))
        data))
    (catch Exception e
      nil)))

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
                         (icon/arrow-u-up-left {:class       "size-4 inline ml-1 text-inherit border-b"
                                                :aria-hidden "true"
                                                :focusable   "false"})
                         [:span.sr-only "Back to reference"]]]))
         :code (fn [ctx {:keys [text info] :as node}]
                 (if-let [hiccup (embed-hiccup node)]
                   hiccup
                   (let [class (when info (str "language-" info))]
                     [:pre [:code {:class class} (or text (md.transform/->text node))]])))
         :plain (partial md.transform/into-markup [:span])
         :html-inline (fn [ctx node]
                        (html/raw
                         (get-in node [:content 0 :text])))
         :html-block (fn [ctx node]
                       (html/raw
                        (get-in node [:content 0 :text])))))

(defn ->hiccup [string]
  (let [[_ md-ast] (parse string)]
    (md.transform/->hiccup transform-ctx (transform-ast md-ast))))

(defn parse-markdown-meta
  "Returns a map with the edn frontmatter and the markdown string under :page/body"
  [string]
  (let [[metadata markdown] (parse-metadata string)]
    (assoc (or metadata {})
           :page/body markdown)))

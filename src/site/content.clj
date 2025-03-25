(ns site.content
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [nextjournal.markdown :as md]
   [nextjournal.markdown.transform :as md.transform])
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

(defn md->hiccup [string]
  (let [s                     (-> string s2sr line-seq)
        [metadata meta-lines] (parse-edn-metadata-headers s)]
    [metadata
     (->> (drop meta-lines s)
          (str/join "\n")
          md/parse
          (md.transform/->hiccup
           (assoc md.transform/default-hiccup-renderers
                  :plain (partial md.transform/into-markup [:span]))))]))

(defn parse [path]
  (let [file               (io/file (str "content/" path "/index.md"))
        [metadata content] (-> file slurp md->hiccup)]
    {:metadata metadata
     :title    (:title metadata)
     :content  content}))

(defn content [page-fn dir req]
  (page-fn
   (parse (str dir "/" (-> req :path-params :slug)))))

(defn format-date
  "Format date in US format (e.g., January 12, 2023)"
  [date-str]
  (let [date (LocalDate/parse date-str)]
    (.format date
             (DateTimeFormatter/ofPattern "MMMM d, yyyy"))))

(defn get-articles
  "Get all articles sorted by date (most recent first)"
  []
  (let [article-dirs (filter #(.isDirectory %)
                             (file-seq (io/file "content/posts")))]
    (->> article-dirs
         (filter #(.exists (io/file (str (.getPath %) "/index.md"))))
         (map (fn [dir]
                (let [slug (.getName dir)
                      {:keys [metadata]} (parse (str "posts/" slug))]
                  (assoc metadata
                         :slug slug
                         :date (:date metadata)))))
         (sort-by :date #(compare %2 %1)))))

(comment
  (:content
   (parse "posts/mobile-security-field-workers"))
  (format-date "2023-01-12")
  (get-articles)
  ;; rcf
  )

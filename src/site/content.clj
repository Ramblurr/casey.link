(ns site.content
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [nextjournal.markdown :as md]
   [nextjournal.markdown.transform :as md.transform]))

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
          md.transform/->hiccup)]))

(defn parse [path]
  (let [file               (io/file (str "content/" path "/index.md"))
        [metadata content] (-> file slurp md->hiccup)]
    {:metadata metadata
     :title    (:title metadata)
     :content  content}))

(defn content [page-fn dir req]
  (page-fn
   (parse (str dir "/" (-> req :path-params :slug)))))

(comment
  (parse "posts/hello")
  ;; rcf
  )

(ns site.content
  (:require
   [clj-org.org :as org]
   [clojure.java.io :as io]))

(defn parse [path]
  (let [file                            (io/file (str "content/" path "/index.org"))
        {:keys [title headers content]} (-> file
                                            slurp
                                            org/parse-org)]
    {:title   title
     :headers headers
     :content content}))

(defn content [page-fn dir req]
  (page-fn
   (parse (str dir "/" (-> req :path-params :slug)))))

(comment
  (parse "posts/hello"))

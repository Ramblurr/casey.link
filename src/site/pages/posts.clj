(ns site.pages.posts)

(defn post [page]
  (assoc page
         :content
         [:main (:content page)]))

(ns site.pages.render)

(defmulti page-content (fn [page req] (:page/kind page)))

(defn page-attrs [page]
  (select-keys page [:page/kind
                     :page/uri
                     :page/resource-path
                     :page/title
                     :page/last-modified
                     :page/description
                     :page/body]))

(defn with-body [page body]
  (-> page
      page-attrs
      (assoc :page/body body)))

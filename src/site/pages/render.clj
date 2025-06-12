(ns site.pages.render)

(defmulti page-content (fn [page req] (:page/kind page)))

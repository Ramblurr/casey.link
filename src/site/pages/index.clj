(ns site.pages.index)

(defn index [req]
  {:title   "Home"
   :uri     "/"
   :content [:main "Hello World!"]})

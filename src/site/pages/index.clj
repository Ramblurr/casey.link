(ns site.pages.index)

(defn index [_]
  {:title   "Index Page"
   :uri     "/"
   :content [:main
             [:h1 "Hello World!"]
             [:a {:href "/posts"} "Posts"]]})

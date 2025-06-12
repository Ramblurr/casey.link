(ns site.pages
  (:require
   [site.pages.about]
   [site.pages.blog-post]
   [site.pages.blog-index]
   [site.pages.home]
   [site.pages.projects]))

(defn get-page-kind [path]
  (cond
    (re-find #"^blog/[a-zA-Z0-9-_]+/index.md" path)
    :page.kind/blog-post

    (re-find #"^about\.md" path)
    :page.kind/about

    :else nil))

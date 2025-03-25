(ns site.pages.index
  (:require
   [site.ui.home :as home]
   [site.content :as content]))

(defn index [_]
  {:title   "Casey Link | Developer, Technical Strategist & NGO Specialist"
   :uri     "/"
   :content (home/home {:articles (content/get-articles)})})

(ns site.pages.index
  (:require
   [site.content :as content]
   [site.ui.home :as home]))

(defn index [_]
  (let [articles (content/get-articles)]
    {:title   "Casey Link | Developer, Technical Strategist & NGO Specialist"
     :uri     "/"
     :content (home/home {:articles articles})}))

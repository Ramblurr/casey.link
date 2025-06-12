(ns site.markdown-test
  (:require
   [clojure.test :refer [are deftest is testing]]
   [site.markdown :as sut]))

(deftest test-parse-edn-frontmatter
  (is (= [{:title "wut", :tags #{:one :two}} 5]
         (sut/parse-edn-frontmatter (line-seq (sut/s2sr "{
  :title \"wut\"
  :tags #{:one :two}
}

# Hello World


This is a markdown post with EDN frontmatter
")))))

  (is (= [nil 0]
         (sut/parse-edn-frontmatter (line-seq (sut/s2sr "# Hello World")))))

  (is (= [nil 0]
         (sut/parse-edn-frontmatter (line-seq (sut/s2sr ""))))))

(defn sidenote-hiccup [note]
  [:div.sidenote-column
   [:span.sidenote
    {:role "doc-footnote", :id "fnref1"}
    [:sup.sidenote-number "1."]
    note
    [:a
     {:role "doc-backlink", :href "#fn1", :class "text-inherit"}
     [:svg
      {:xmlns   "http://www.w3.org/2000/svg",
       :viewBox "0 0 256 256",
       :role    "img"
       :class   "size-4 inline ml-1 text-inherit border-b "}
      [:path {:fill "none", :d "M0 0h256v256H0z"}]
      [:path
       {:fill            "none",
        :stroke          "currentColor",
        :stroke-linecap  "round",
        :stroke-linejoin "round",
        :stroke-width    "16",
        :d               "M80 136 32 88l48-48"}]
      [:path
       {:fill            "none",
        :stroke          "currentColor",
        :stroke-linecap  "round",
        :stroke-linejoin "round",
        :stroke-width    "16",
        :d
        "M80 200h88a56 56 0 0 0 56-56h0a56 56 0 0 0-56-56H32"}]]]]])

(deftest test-markdown-parsing
  (testing "Images"
    (are [markdown expected] (= [:div expected]  (sut/->hiccup markdown))
      "sanity-check"                             [:p "sanity-check"]
      "![](./image.png)"                         [:figure.image [:img {:src "./image.png" :title nil :alt ""}] nil]
      "![](./image.png \"A title\")"             [:figure.image [:img {:src "./image.png" :title "A title" :alt ""}] nil]
      "![alt](./image.png)"                      [:figure.image [:img {:src "./image.png" :title nil :alt "alt"}] nil]
      "![alt](./image.png \"A title\")"          [:figure.image [:img {:src "./image.png" :title "A title" :alt "alt"}] nil]
      "![alt](./image.png)\nCaption"             [:figure.image [:img {:src "./image.png" :title nil :alt "alt"}]
                                                  [:figcaption.text-center.mt-1 "Caption"]]
      "![alt](./image.png \"A title\")\nCaption" [:figure.image [:img {:src "./image.png" :title "A title" :alt "alt"}]
                                                  [:figcaption.text-center.mt-1 "Caption"]]
      "![alt](./image.png \"A title\")\nCaption" [:figure.image [:img {:src "./image.png" :title "A title" :alt "alt"}]
                                                  [:figcaption.text-center.mt-1 "Caption"]]
      "foo[^note1] bar\n[^note1]: a note"        [:div.sidenote-container
                                                  [:p
                                                   "foo"
                                                   [:a.sidenote-ref
                                                    {:id "fn1", :href "#fnref1", :role "doc-noteref"}
                                                    [:sup {:data-label "note1"} "1"]]
                                                   " bar"]
                                                  (sidenote-hiccup "a note")]
      "```clojure\n(+ 1 1)\n```"                 [:pre [:code {:class "language-clojure"} "(+ 1 1)\n"]])))

(ns site.feed-test
  (:require
   [clojure.string :as str]
   [clojure.test :refer [are deftest is testing]]
   [site.feed :as sut]))

(deftest date-str->rfc3339-test
  (testing "converts date strings to RFC 3339 format"
    (are [input expected] (= expected (sut/date-str->rfc3339 input))
      "2024-10-04" "2024-10-04T00:00:00Z"
      "2022-01-01" "2022-01-01T00:00:00Z"
      "2025-12-31" "2025-12-31T00:00:00Z")))

(deftest extract-domain-test
  (testing "extracts domain from various URL formats"
    (are [input expected] (= expected (sut/extract-domain input))
      "https://casey.link"     "casey.link"
      "http://casey.link"      "casey.link"
      "https://casey.link/"    "casey.link"
      "https://example.com"    "example.com"
      "https://sub.domain.org" "sub.domain.org")))

(deftest feed-id-test
  (testing "generates correct tag URI for feed"
    (is (= "tag:casey.link,2022:/atom/articles"
           (sut/feed-id "https://casey.link")))))

(deftest entry-id-test
  (testing "generates correct tag URI for entry"
    (is (= "tag:casey.link,2024-10-04:/blog/test/"
           (sut/entry-id "https://casey.link"
                         {:blog/date "2024-10-04"
                          :page/uri  "/blog/test/"})))))

(deftest post-url-test
  (testing "generates absolute URL for post"
    (is (= "https://casey.link/blog/my-post/"
           (sut/post-url "https://casey.link"
                         {:page/uri "/blog/my-post/"})))))

(deftest author-element-test
  (testing "generates correct author element"
    (is (= [:author [:name "Casey Link"]]
           (sut/author-element "Casey Link")))))

(deftest link-element-test
  (testing "generates self-closing link element"
    (let [result (sut/link-element "https://example.com" "self" "application/atom+xml")]
      (is (str/includes? (str result) "href=\"https://example.com\""))
      (is (str/includes? (str result) "rel=\"self\""))
      (is (str/includes? (str result) "/>")))))

(deftest feed-updated-test
  (testing "returns nil for empty posts"
    (is (nil? (sut/feed-updated []))))

  (testing "uses first post's modified date if available"
    (is (= "2024-10-04T00:00:00Z"
           (sut/feed-updated [{:blog/date     "2024-01-01"
                               :blog/modified "2024-10-04"}]))))

  (testing "falls back to date if no modified"
    (is (= "2024-01-01T00:00:00Z"
           (sut/feed-updated [{:blog/date "2024-01-01"}])))))

(deftest entry-element-test
  (testing "generates entry with all fields"
    (let [post   {:page/title       "Test Post"
                  :page/uri         "/blog/test/"
                  :page/body        "Hello world"
                  :blog/date        "2024-10-04"
                  :blog/author      "Casey Link"
                  :blog/description "A test post"}
          result (sut/entry-element "https://casey.link" post)]
      (is (= :entry (first result)))
      (is (some #(= [:title "Test Post"] %) result))
      (is (some #(= [:id "tag:casey.link,2024-10-04:/blog/test/"] %) result))
      (is (some #(= [:published "2024-10-04T00:00:00Z"] %) result))
      (is (some #(= [:updated "2024-10-04T00:00:00Z"] %) result))
      (is (some #(= [:author [:name "Casey Link"]] %) result))
      (is (some #(= [:summary "A test post"] %) result))))

  (testing "uses modified date for updated if available"
    (let [post   {:page/title       "Test Post"
                  :page/uri         "/blog/test/"
                  :page/body        "Hello"
                  :blog/date        "2024-01-01"
                  :blog/modified    "2024-10-04"
                  :blog/author      "Casey Link"
                  :blog/description "A test post"}
          result (sut/entry-element "https://casey.link" post)]
      (is (some #(= [:published "2024-01-01T00:00:00Z"] %) result))
      (is (some #(= [:updated "2024-10-04T00:00:00Z"] %) result)))))

(deftest feed-element-test
  (testing "generates feed with correct structure"
    (let [posts  [{:page/title       "Post 1"
                   :page/uri         "/blog/post1/"
                   :page/body        "Content 1"
                   :blog/date        "2024-10-04"
                   :blog/author      "Casey Link"
                   :blog/description "First post"}]
          result (sut/feed-element "https://casey.link" posts)]
      (is (= :feed (first result)))
      (is (= {:xmlns "http://www.w3.org/2005/Atom"} (second result)))
      (is (some #(= [:title "Casey Link's Weblog"] %) result))
      (is (some #(= [:id "tag:casey.link,2022:/atom/articles"] %) result)))))

(deftest generate-feed-test
  (testing "generates valid XML string"
    (let [posts [{:page/title       "Post 1"
                  :page/uri         "/blog/post1/"
                  :page/body        "Content"
                  :blog/date        "2024-10-04"
                  :blog/author      "Casey Link"
                  :blog/description "First post"}]
          xml   (sut/generate-feed "https://casey.link" posts)]
      (is (str/starts-with? xml "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"))
      (is (str/includes? xml "<feed xmlns=\"http://www.w3.org/2005/Atom\">"))
      (is (str/includes? xml "<title>Casey Link's Weblog</title>"))
      (is (str/includes? xml "<entry>"))
      (is (str/includes? xml "<![CDATA[")))))

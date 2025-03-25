(ns site.content-test
  (:require [site.content :as sut]
            [clojure.test :refer [deftest is testing]]))

(deftest test-parse-edn-metadata-headers
  (is (= [{:title "wut", :tags #{:one :two}} 5]
         (sut/parse-edn-metadata-headers (line-seq (sut/s2sr "{
  :title \"wut\"
  :tags #{:one :two}
}

# Hello World


This is a markdown post with EDN frontmatter
")))))

  (is (= [nil 0]
         (sut/parse-edn-metadata-headers (line-seq (sut/s2sr "# Hello World")))))

  (is (= [nil 0]
         (sut/parse-edn-metadata-headers (line-seq (sut/s2sr ""))))))

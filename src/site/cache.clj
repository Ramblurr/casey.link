(ns site.cache
  (:require
   [site.crypto :as crypto]
   [ring.middleware.not-modified :as ring.not-modified]))

(defn cache-get [!cache handler request {:keys [key-fn]
                                         :or   {key-fn #(:uri %)}}]
  (let [cache-key (key-fn request)
        cached    (@!cache cache-key)]
    (if (nil? cached)
      ;; Cache miss
      (when-some [response (handler request)]
        (let [etag     (crypto/digest (:body response))
              headers  {"ETag"          etag
                        "Cache-Control" "public, max-age=300"}
              response (update response :headers merge headers)]
          (vswap! !cache assoc cache-key response)
          (ring.not-modified/not-modified-response response request)))
      ;; Cache hit
      (ring.not-modified/not-modified-response cached request))))

(defn wrap-cache [handler {:keys [exclude? dev?]
                           :or   {exclude? (constantly false)}
                           :as   opts}]
  (let [!cache (volatile! {})]
    (fn [request]
      (if dev?
        (handler request)
        (if (exclude? request)
          (handler request)
          (cache-get !cache handler request opts))))))

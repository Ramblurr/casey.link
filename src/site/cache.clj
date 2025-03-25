(ns site.cache
  (:require [ring.middleware.not-modified :as ring.not-modified])
  (:import (java.io File)
           [java.time Instant]
           (java.time ZoneId)
           (java.time.format DateTimeFormatter)
           (java.time.temporal TemporalAccessor)))

(def UTC  (ZoneId/of "UTC"))

(def ^:dynamic *seen*
  nil)

(defn mark-seen! ^File [file]
  (when *seen*
    (vswap! *seen* conj file))
  file)

(defn midnight-today []
  (->
   (java.time.LocalDate/now UTC)
   (.atStartOfDay UTC)
   (.getLong java.time.temporal.ChronoField/INSTANT_SECONDS)
   (* 1000)))

(defn get-modified [files]
  (max (transduce (map #(.lastModified ^File %)) max 0 files) (midnight-today)))

(defn format-ta [^TemporalAccessor ta format]
  (when ta
    (let [^DateTimeFormatter format (cond-> format
                                      (string? format) DateTimeFormatter/ofPattern)
          format                    (.withZone format UTC)]
      (.format format ta))))

(defn format-last-modified [last-modified]
  (format-ta (Instant/ofEpochMilli last-modified) DateTimeFormatter/RFC_1123_DATE_TIME))

(defn cache-miss [!cache cache-key actual-last-modified handler request]
  (when-some [response (handler request)]
    (let [headers  {"Last-Modified" (format-last-modified actual-last-modified)
                    "Cache-Control" "no-cache, max-age=315360000"}
          response (update response :headers merge headers)]
      (vswap! !cache assoc cache-key
              {:last-modified actual-last-modified
               :files         @*seen*
               :response      response})
      response)))

(defn cache-get [!cache handler request {:keys [key-fn]
                                         :or   {key-fn #(:uri %)}}]
  (binding [*seen* (volatile! #{})]
    (let [cache-key                              (key-fn request)
          {:keys [last-modified files response]} (@!cache cache-key)
          actual-last-modified                   (get-modified files)]
      (if (or (nil? response) (> actual-last-modified last-modified))
        (do
          (prn "MISS!" cache-key)
          (cache-miss !cache cache-key actual-last-modified handler request))
        (do
          (prn "HIT!" cache-key)
          (ring.not-modified/not-modified-response response request))))))

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

(ns site.crypto
  (:require
   [clojure.java.io :as io])
  (:import
   [java.security MessageDigest SecureRandom]
   [java.util Base64 Base64$Encoder]
   [javax.crypto Mac]
   [javax.crypto.spec SecretKeySpec]))

(def ^Base64$Encoder base64-encoder
  (.withoutPadding (Base64/getUrlEncoder)))

(defn ->base64
  ^String [^byte/1 b]
  (.encodeToString base64-encoder b))

(defn hash-stream-data ^MessageDigest
  [^String algo ^java.io.InputStream in]
  (when in
    (let [d      (MessageDigest/getInstance algo)
          buffer (byte-array 5120)]
      (loop []
        (let [read-size (.read in buffer 0 5120)]
          (when-not (= read-size -1)
            (.update d ^bytes buffer 0 read-size)
            (recur))))
      d)))

(defn sha384
  ^bytes [^bytes input-bytes]
  (-> (doto (MessageDigest/getInstance "SHA-384")
        (.update input-bytes))
      (.digest)))

(defn sha384-stream
  ^bytes [^bytes ^java.io.InputStream in]
  (.digest (hash-stream-data "SHA-384" in)))

(defn sha384-resource [path]
  (if-let [resource (io/resource path)]
    (str "sha384-"
         (-> resource
             io/input-stream
             sha384-stream
             ->base64))
    (throw (ex-info "Cannot load resource from classpath" {:path path}))))

(defn digest
  "Digest function based on Clojure's hash."
  ;; Copyright © 2025 Anders Murphy
  ;; https://github.com/andersmurphy/hyperlith/
  ;; SPDX-License-Identifier: MIT
  [data]
  ;; Note: hashCode is not guaranteed consistent between JVM
  ;; executions except in the case for strings. This is why we
  ;; convert to a string first.
  (Integer/toHexString (hash (str data))))

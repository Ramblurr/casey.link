(ns site.headers
  (:require [clojure.string :as str]))

(def self "'self'")
(def none "'none'")
(def unsafe-inline "'unsafe-inline'")
(def unsafe-eval "'unsafe-eval'")

(def csp-data
  {:base-uri        [self]
   ;; Chrome and Safari do not allow redirects after submitting a form
   ;; unless the destination URL is listed in the form-action CSP rule,
   ;; even if it is a GET redirect that does not contain the original
   ;; form data.
   :form-action     [self]
   :default-src     [none]
   :media-src       [self "https: data:"]
   :script-src      [self unsafe-eval "'sha256-BP34lJXTeOsH+EiTxSXNkA6VgmcWT5BrcJYrz/5gqtU='" "'sha256-azbXrmMNM3r7AQYITy4uGJ1+wQ3JVANjvyLuLT6N1Sg='"]
   :img-src         [self "https: data:"]
   :font-src        [self]
   :connect-src     [self]
   :style-src       [self unsafe-inline]
   :style-src-elem  [self unsafe-inline]
   :frame-ancestors [none]})

(defn csp-data->str [csp-data]
  (reduce
   (fn [acc [k v]] (str acc (name k) " " (str/join " " v) ";"))
   ""
   csp-data))

(def strict-transport
  "Forces https, including on subdomains. Prevents attacker from using
  compromised subdomain."
  "max-age=63072000;includeSubDomains;preload")

(def default-headers
  {"Content-Type"              "text/html; charset=UTF-8"
   "Strict-Transport-Security" strict-transport
   "Content-Security-Policy"   (csp-data->str csp-data)
   "Referrer-Policy"           "no-referrer"
   "X-Content-Type-Options"    "nosniff"
   "X-Frame-Options"           "deny"
   "Cache-Control"             "no-cache, must-revalidate"})

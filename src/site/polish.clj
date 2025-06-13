(ns site.polish
  (:require
   [clojure.edn :as edn]
   [clojure.java.shell :as shell]
   [clojure.string :as str]
   [clojure.java.io :as io]
   [site.ui.core :as uic]
   [clojure.walk :as walk])
  (:import (java.io File)))

(defn sh [& args]
  (let [{:keys [exit] :as res} (apply shell/sh args)]
    (if (= 0 exit)
      res
      (throw
       (ex-info
        (str "External process failed: " (str/join " " args) " returned " exit)
        (assoc res :command args))))))

(defn resize [^File file w h]
  (let [[w h] (cond (str/index-of (.getName file) "@2x") [(quot w 2) (quot h 2)]
                    (>= w 1088)                          [(quot w 2) (quot h 2)]
                    (> h 700)                            [(quot w 2) (quot h 2)]
                    :else                                [w h])]
    (if (str/index-of (.getName file) "@hover")
      [w (quot h 2)]
      [w h])))

(defn svg-dimensions [file]
  (let [body       (slurp file)
        head       (re-find #"<svg[^>]+>" body)
        [_ width]  (re-find #"width=['\"]([0-9.\-]+)['\"]" head)
        [_ height] (re-find #"height=['\"]([0-9.\-]+)['\"]" head)]
    (when (and width height)
      [(edn/read-string width) (edn/read-string height)])))

(def image-dimensions
  (fn [path]
    (try
      (let [file (io/file path)]
        (when (.exists file)
          (or
           (when (str/ends-with? (.getName file) ".svg")
             (svg-dimensions file))
           (let [out   (:out (sh "convert" (.getPath file) "-ping" "-format" "[%w,%h]" "info:"))
                 [w h] (edn/read-string out)]
             (resize file w h)))))
      (catch Exception e
        (.printStackTrace e)
        nil))))

(defn norm [element]
  (if (vector? element)
    (uic/norm element)
    nil))

(defn find-file [page uri]
  (when-not (str/starts-with? uri "http")
    (let [file-path (str (:path page "public") "/" uri)
          file      (io/as-file (io/resource file-path))]
      (when (and file (.exists file))
        file))))

(defn timestamp-url [url file]
  (let [file (io/file file)]
    (if (.exists file)
      (let [modified (quot (.lastModified file) 1000)]
        (str url (if (str/index-of url "?") "&" "?") "t=" modified))
      url)))

(defn polish-img [page [tag attrs children]]
  (when (and (not (str/blank? (:src attrs))))
    (when-some [file (find-file page (:src attrs))]
      (let [[w h]  (image-dimensions file)
            style' (str "aspect-ratio: " w "/" h "; " (:style attrs))
            src'   (timestamp-url (:src attrs) file)]
        [tag
         (cond-> attrs
           (not (contains? attrs :sizes)) (assoc :width w :height h)
           true                           (assoc :src src' :style style'))
         children]))))

(defn polish-element [{:keys [base-url]} page element]
  (assert base-url "base-url must be provided in config")
  (or
   (when-some [[tag attrs children] (norm element)]
     (cond
       (= tag :img)
       (polish-img page [tag attrs children])

       (and (= :a tag) (:href attrs))
       (when (and (str/starts-with? (:href attrs) "http")
                  (not (str/starts-with? (:href attrs) base-url)))
         [tag (assoc attrs :target "_blank")  children])

       :else nil))

   element))

(defn hiccup [page config]
  (update page :page/body
          (fn [form]
            (walk/postwalk #(polish-element config page %) form))))

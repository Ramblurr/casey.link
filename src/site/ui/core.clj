(ns site.ui.core
  (:require [clojure.string :as str]))

(defn cs [& names]
  (str/join " " (filter identity names)))

(defn merge-attrs [orig-map & {:as extra}]
  (reduce (fn [acc [k v]]
            (case k
              :class (update acc :class #(str v " " %))
              (assoc acc k v))) (or orig-map {}) extra))
(defn attr+children [coll]
  (when (coll? coll)
    (let [[a & xs] coll
          attr     (when (map? a) a)]
      [attr (if attr xs coll)])))

(defn user-attr? [x]
  (and (keyword? x)
       (->> x name (re-find #"^-[^\s\d]+"))))

(defn unwrapped-children [children]
  (let [fc (nth children 0 nil)]
    (if (and
         (seq? children)
         (= 1 (count children))
         (seq? fc)
         (seq fc))
      fc
      children)))

(defn extract
  "Extracts component options from normal html attributes and children elements.
  Returns a vector of [options html-attributes children]"
  ;; it is important that we preserve the type of the attr map
  ;; in case the user has supplied an array-map because the order of their attributes is important
  [args]
  (when (coll? args)
    (let [[attr* children] (attr+children args)
          user-ks          (some->> attr*
                                    keys
                                    (filter user-attr?)
                                    (into #{}))
          ;; Calling dissoc on an array map always yields an array map
          attr             (apply dissoc attr* user-ks)
          opts             (select-keys attr* (into [] user-ks))
          supplied-opts    (->> opts
                                (map (fn [[k v]]
                                       [(-> k name (subs 1) keyword) v]))
                                (into {}))]
      [supplied-opts
       attr
       (->> children
            (remove nil?)
            unwrapped-children)])))

(defn norm
  "Normalizes the hiccup element to a vector of [tag attrs & children]"
  [hiccup]
  (let [[tag & rest]     hiccup
        [attrs children] (if (map? (first rest))
                           [(first rest) (next rest)]
                           [nil rest])]
    [tag attrs children]))

(defn merge-attrs*
  ^clojure.lang.IPersistentMap [orig-map & {:as extra}]
  (reduce (fn [acc [k v]]
            (case k
              :class (update acc :class #(str v " " %))
              (assoc acc k v))) orig-map extra))

(defn assoc-attr
  "Assoc attributes to the hiccup element"
  [hiccup & {:as args}]
  (update-in (norm hiccup) [1]
             merge-attrs*
             args))

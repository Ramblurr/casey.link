(ns site.html
  (:require [dev.onionpancakes.chassis.compiler :as cc]
            [dev.onionpancakes.chassis.core :as h]))

(cc/set-warn-on-ambig-attrs!)

(def doctype-html5 h/doctype-html5)

(def ->str h/html)

(def raw h/raw)

(defmacro html
  "Compiles html."
  [& hiccups]
  (let [node (vec hiccups)]
    `(cc/compile ~node)))

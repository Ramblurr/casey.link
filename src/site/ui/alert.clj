(ns site.ui.alert
  (:require
   [site.ui.core :as uic]
   [site.ui.icons :as icon]

   [dev.onionpancakes.chassis.core :as c]))

(def icon {:warning icon/warning})

(def border-color
  {:warning "border-yellow-500"})

(def color
  {:warning "text-yellow-500"})

(defmethod c/resolve-alias ::Alert
  [_ {:keys [_title _type] :as attrs} content]
  (let [type (keyword _type)]
    [:aside {:class (uic/cs "not-prose py-2 px-4 mb-0 border-l-4" (border-color type))}
     (when _title
       [:h2 {:class (uic/cs "font-bold mb-2" (color type))}
        ((get icon type) {:class "w-5 h-5 inline-block mr-2 fill-current stroke-current"})
        _title])
     [:p content]]))

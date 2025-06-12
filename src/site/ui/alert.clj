(ns site.ui.alert
  (:require
   [site.ui.core :as uic]
   [site.ui.icons :as icon]

   [dev.onionpancakes.chassis.core :as c]))

(def icon {:warning icon/warning
           :info    icon/info})

(def border-color
  {:warning "border-yellow-500"
   :info    "border-blue-500"})

(def color
  {:warning "text-yellow-500"
   :info    "text-blue-500"})

(def Alert ::Alert)
(defmethod c/resolve-alias ::Alert
  [_ {::keys [title type] :as attrs} content]
  [:aside {:class (uic/cs "not-prose py-2 px-4 mb-0 border-l-4" (border-color type))}
   (when title
     [:h2 {:class (uic/cs "font-bold mb-2" (color type))}
      ((get icon type) {:class "w-5 h-5 inline-block mr-2 fill-current stroke-current"})
      title])
   [:p content]])

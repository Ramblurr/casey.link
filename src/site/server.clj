(ns site.server
  (:require [aero.core :as aero]
            [site.content :as content]
            [clojure.java.io :as io]
            [donut.system :as ds]
            [org.httpkit.server :as server]
            [reitit.ring :as rr]
            [ring.middleware.cookies :as ring.cookies]
            [ring.middleware.head :as ring.head]
            [ring.middleware.not-modified :as ring.not-modified]
            [ring.middleware.params :as ring.params]
            [ring.util.io :as ring-io]
            [ring.util.mime-type :as ring-mime]
            [ring.util.time :as ring-time]
            [site.cache :as cache]
            [site.headers :as headers]
            [site.html :as html]
            [site.pages :as pages]
            [site.pages.posts :as posts]
            [site.pages.index :as index])
  (:import (java.io File)))

(defn html-response [page-fn]
  (fn [req]
    {:status  200
     :headers headers/default-headers
     :body    (-> (page-fn req)
                  pages/shell
                  :content
                  html/->str)}))

(def routes ["" {:middleware [[cache/wrap-cache {}]]}
             ["/" {:handler (html-response index/index)}]
             ["/blog/:slug" {:handler (html-response #(content/content posts/post "posts" %))}]])

(defn find-file ^File [path]
  (when-let [file ^File (io/as-file (io/resource path))]
    (when (.exists file)
      (let [file (if (.isDirectory file)
                   (io/file file "index.html")
                   file)]
        (when (.exists file)
          file)))))

(def cache-levels #{:no-cache :immutable})
(defn serve-files [dir {:keys [cache-level] :as _opts
                        :or   {cache-level :no-cache}}]
  (fn [req]
    (if-some [file (find-file (str dir "/" (:uri req)))]
      {:status  200
       :body    file
       :headers {"Content-Length" (.length file)
                 "Last-Modified"  (ring-time/format-date (ring-io/last-modified-date file))
                 "Cache-Control"  (condp = cache-level :immutable "max-age=31536000,immutable,public" :no-cache "no-cache")
                 "Content-Type"   (ring-mime/ext-mime-type (.getName file))}}
      nil)))

(def router
  (rr/router routes
             {:data {:middleware [ring.params/wrap-params
                                  ring.cookies/wrap-cookies
                                  ring.head/wrap-head]}}))

(defn not-found-handler [req]
  {:status 404
   :body   "Not Found"})

(defn create-asset-handler [dir opts]
  (let [handler (serve-files dir opts)]
    (ring.not-modified/wrap-not-modified handler)))

(def base-system
  {::ds/defs
   {:env {}
    :site
    {:handler #::ds{:start (fn [_]
                             (rr/ring-handler router
                                              (rr/routes
                                               (create-asset-handler "public" {:cache-level :immutable})
                                               (rr/create-default-handler {:not-found not-found-handler}))))}
     :server  #::ds{:start  (fn [{config ::ds/config}]
                              (let [instance (server/run-server (:handler config)
                                                                {:port                 (:port config)
                                                                 :ip                   (:host config)
                                                                 :legacy-return-value? false})]
                                (println (format "started listening at %s:%s" (:host config) (:port config)))
                                instance))
                    :stop   (fn [{server ::ds/instance}]
                              (when server
                                (server/server-stop! server)))
                    :config {:handler (ds/local-ref [:handler])
                             :host    (ds/ref [:env :host])
                             :port    (ds/ref [:env :port])}}}}})

(defn env-config [& [profile]]
  (aero/read-config (io/resource "site/env.edn")
                    (when profile {:profile profile})))

(defmethod ds/named-system ::base
  [_]
  base-system)

(defmethod ds/named-system ::dev
  [_]
  (ds/system ::base {[:env] (env-config :dev)}))

(defn -main [& _args]
  (ds/start ::dev))

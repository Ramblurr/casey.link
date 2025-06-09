(ns site.server
  (:require [aero.core :as aero]
            [site.sitemap :as sitemap]
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
            [site.ui :as ui]
            [site.polish :as polish]
            [site.dev :as dev]
            [site.pages.index :as index]
            [site.pages.about :as about]
            [site.pages.projects :as projects]
            [site.pages.articles :as articles])
  (:import (java.io File)))

(defn html-response [config page-fn]
  (fn [req]
    {:status  200
     :headers headers/default-headers
     :body    (-> (page-fn req)
                  ui/shell
                  (polish/hiccup config)
                  :content
                  html/->str)}))

(defn routes [config]
  ["" {:middleware [[cache/wrap-cache config]]}
   ["/" {:handler (html-response config index/index)}]
   (when (:dev? config)
     (dev/routes config))
   ["/about" {:handler (html-response config about/about)}]
   ["/projects" {:handler (html-response config projects/projects)}]
   ["/sitemap.xml" {:get              (sitemap/create-sitemap-handler config)
                    :sitemap/exclude? true}]
   ["/articles"
    ["" {:handler (html-response config articles/articles-index)}]
    (articles/article-routes (partial html-response config))]])

(defn find-file ^File [path]
  (when-let [file ^File (io/as-file (io/resource path))]
    (when (.exists file)
      (let [file (if (.isDirectory file)
                   (io/file file "index.html")
                   file)]
        (when (.exists file)
          file)))))

;; (def cache-levels #{:no-cache :immutable})
(defn serve-files [dir {:keys [cache-level] :as _opts
                        :or   {cache-level :no-cache}}]
  (fn [req]
    (if-some [file (find-file (str dir (:uri req)))]
      {:status  200
       :body    file
       :headers {"Content-Length" (.length file)
                 "Last-Modified"  (ring-time/format-date (ring-io/last-modified-date file))
                 "Cache-Control"  (condp = cache-level :immutable "max-age=31536000,immutable,public" :no-cache "no-cache")
                 "Content-Type"   (ring-mime/ext-mime-type (.getName file))}}
      nil)))

(defn not-found-handler [_req]
  {:status 404
   :body   "Not Found"})

(defn create-asset-handler [dir opts]
  (let [handler (serve-files dir opts)]
    (ring.not-modified/wrap-not-modified handler)))

(defn folder-redirects-handler [{:keys [uri] :as req}]
  (when (re-matches #"(/articles/[^/]+|/projects/[^/]+)" uri)
    {:status  301
     :headers {"Location" (str uri "/")}}))

(def base-system
  {::ds/defs
   {:env {}
    :site
    {:handler #::ds{:start  (fn [{config ::ds/config}]
                              (rr/ring-handler (rr/router (routes config)
                                                          {:data {:middleware [ring.params/wrap-params
                                                                               ring.cookies/wrap-cookies
                                                                               ring.head/wrap-head]}})
                                               (rr/routes
                                                ;; (create-asset-handler "content" {:cache-level :immutable})
                                                (create-asset-handler "public" {:cache-level :immutable})
                                                folder-redirects-handler
                                                (rr/create-default-handler {:not-found not-found-handler}))))
                    :config {:dev?     (ds/ref [:env :dev?])
                             :base-url (ds/ref [:env :base-url])}}
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
                             :port    (ds/ref [:env :port])}}
     :watcher #::ds{:start  (fn [{config ::ds/config}]
                              (when (:dev? config)
                                (let [watcher (dev/start-watcher config)]
                                  (println "Watching for changes.")
                                  watcher)))
                    :stop   (fn [{watcher ::ds/instance}]
                              (when watcher
                                (dev/stop-watcher watcher)))
                    :config {:dev?        (ds/ref [:env :dev?])
                             :watch-paths (ds/ref [:env :watch-paths])
                             :base-url    (ds/ref [:env :base-url])}}}}})

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

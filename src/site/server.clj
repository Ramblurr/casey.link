(ns site.server
  (:require
   [aero.core :as aero]
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
   [site.content :as content2]
   [site.db :as db]
   [site.dev :as dev]
   [site.pages :as pages]
   [site.sitemap :as sitemap])
  (:import
   (java.io File))
  (:gen-class))

(defn routes [config]
  ["" {:middleware [[cache/wrap-cache config]
                    [db/wrap-datomic config]]}
   (when (:dev? config)
     (dev/routes config))

   ["/sitemap.xml" {:get              (sitemap/create-sitemap-handler config)
                    :sitemap/exclude? true}]
   (content2/routes (assoc config
                           :get-page-kind pages/get-page-kind
                           :render-page pages/render-page))
   #_[(urls/url-for :url/home) {:handler (html-response config index/index)}]])

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
  (when (re-matches #"(/blog/[^/]+|/projects/[^/]+)" uri)
    {:status  301
     :headers {"Location" (str uri "/")}}))

(def base-system
  {::ds/defs
   {:env {}
    :site
    {:datomic #::ds{:start (fn [{config ::ds/config}]
                             (db/create-database "datomic:mem://site"))
                    :stop  (fn [{conn ::ds/instance}]
                             (when conn
                               (db/close conn)))}

     :handler #::ds{:start  (fn [{config ::ds/config}]
                              (rr/ring-handler (rr/router (routes config)
                                                          {:data {:middleware [ring.params/wrap-params
                                                                               ring.cookies/wrap-cookies
                                                                               ring.head/wrap-head]}})
                                               (rr/routes
                                                ;; (create-asset-handler "content" {:cache-level :immutable})
                                                ;; (create-asset-handler "public" {:cache-level :immutable})
                                                ;; (content2/request-handler config (partial render-page config))
                                                folder-redirects-handler
                                                (rr/create-default-handler {:not-found not-found-handler}))))
                    :config {:dev?     (ds/ref [:env :dev?])
                             :base-url (ds/ref [:env :base-url])
                             :conn     (ds/local-ref [:datomic])}}
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

(defmethod ds/named-system ::prod
  [_]
  (ds/system ::base {[:env] (env-config :prod)}))

(require '[site.class-path :as cp])
(defn -main [& _args]
  (prn (cp/file-metadata-on-class-path))
  #_(ds/start ::prod))

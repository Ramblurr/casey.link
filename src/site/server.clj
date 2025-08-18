(ns site.server
  (:require
   [aero.core :as aero]
   [clojure.java.io :as io]
   [donut.system :as ds]
   [org.httpkit.server :as server]
   [reitit.ring :as rr]
   [ring.middleware.cookies :as ring.cookies]
   [ring.middleware.params :as ring.params]
   [site.cache :as cache]
   [site.compression :as compression]
   [site.content :as content]
   [site.db :as db]
   [site.dev :as dev]
   [site.pages :as pages]
   [site.pages.render :as render]
   [site.sitemap :as sitemap])
  (:import
   [java.io File]
   [java.net SocketAddress StandardProtocolFamily UnixDomainSocketAddress]
   [java.nio.channels ServerSocketChannel SocketChannel])
  (:gen-class))

(defn routes [config]
  (let [config (assoc config
                      :get-page-kind pages/get-page-kind
                      :render-page render/render-page)]
    [""
     (content/asset-routes config)
     ["" {:middleware [[cache/wrap-cache config]
                       [db/wrap-datomic config]]}
      (when (:dev? config)
        (dev/routes config))

      ["/sitemap.xml" {:get              (sitemap/create-sitemap-handler config)
                       :sitemap/exclude? true}]
      (content/page-routes config)]]))

(defn not-found-handler [_req]
  {:status 404
   :body   "Not Found"})

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
                                                                               compression/wrap-compression]}})
                                               (rr/routes
                                                folder-redirects-handler
                                                (rr/create-default-handler {:not-found not-found-handler}))))
                    :config {:dev?     (ds/ref [:env :dev?])
                             :base-url (ds/ref [:env :base-url])
                             :conn     (ds/local-ref [:datomic])}}
     :server  #::ds{:start  (fn [{config ::ds/config}]
                              (let [{:keys [unix-socket port host handler]} config
                                    hk-conf                                 (merge {:legacy-return-value? false}
                                                                                   (if unix-socket
                                                                                     {:address-finder  (fn []         (UnixDomainSocketAddress/of unix-socket))
                                                                                      :channel-factory (fn [_address] (ServerSocketChannel/open StandardProtocolFamily/UNIX))}
                                                                                     {:port port
                                                                                      :ip   host}))]
                                (println (format "starting listening at %s" (if unix-socket  unix-socket (str host ":" port))))
                                (server/run-server handler hk-conf)))
                    :stop   (fn [{server ::ds/instance}]
                              (when server
                                (server/server-stop! server)))
                    :config {:handler     (ds/local-ref [:handler])
                             :unix-socket (ds/ref [:env :unix-socket])
                             :host        (ds/ref [:env :host])
                             :port        (ds/ref [:env :port])}}
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

(defn -main [& _args]
  (ds/start ::prod))

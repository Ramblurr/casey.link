(ns site.server
  (:require
   [aero.core :as aero]
   [clojure.java.io :as io]
   [donut.system :as ds]
   [org.httpkit.server :as server]
   [reitit.ring :as rr]
   [ring.middleware.cookies :as ring.cookies]
   [ring.middleware.head :as ring.head]
   [ring.middleware.params :as ring.params]
   [site.cache :as cache]
   [site.content :as content2]
   [site.db :as db]
   [site.dev :as dev]
   [site.pages :as pages]
   [site.pages.render :as render]
   [site.sitemap :as sitemap])
  (:import
   (java.io File))
  (:gen-class))

(defn routes [config]
  [""
   ["" {:middleware [[cache/wrap-cache config]
                     [db/wrap-datomic config]]}
    (when (:dev? config)
      (dev/routes config))

    ["/sitemap.xml" {:get              (sitemap/create-sitemap-handler config)
                     :sitemap/exclude? true}]
    (content2/page-routes (assoc config
                                 :get-page-kind pages/get-page-kind
                                 :render-page render/render-page))]
   (content2/asset-routes (assoc config
                                 :get-page-kind pages/get-page-kind
                                 :render-page render/render-page))])

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
                                                                               ring.head/wrap-head]}})
                                               (rr/routes
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

(defn -main [& _args]
  (ds/start ::prod))

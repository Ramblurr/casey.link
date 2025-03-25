(ns site.server
  (:require
   [clojure.java.io :as io]
   [aero.core :as aero]
   [donut.system :as ds]
   [jsonista.core :as j]
   [org.httpkit.server :as server]
   [starfederation.datastar.clojure.api :as d*]
   [starfederation.datastar.clojure.adapter.http-kit :refer [->sse-response on-open on-close]]
   [reitit.ring :as rr]
   [dev.onionpancakes.chassis.core :as h]
   [clojure.string :as str]
   [jsonista.core :as json]))

(def base-system
  {::ds/defs
   {:env {}
    :site
    {:handler (fn [req]
                {:status 200 :body "Hello, World!"})
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

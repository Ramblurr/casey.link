(ns site.dev
  (:require
   [site.db :as db]
   [datomic.api :as d]
   [site.pages.render :as render]
   [clojure.java.io :as io]
   [starfederation.datastar.clojure.api :as d*]
   [starfederation.datastar.clojure.adapter.http-kit :as hk-gen]))

(def !connections (atom {}))

(defn broadcast-fragment! [fragment]
  (doseq [c @!connections]
    (d*/merge-fragment! c fragment)))

(defn re-render! []
  (doseq [[sse-gen {:keys [render]}] @!connections]
    (render)))

(defn reload! []
  (doseq [[sse-gen _] @!connections]
    (d*/execute-script! sse-gen "window.location.reload();")))

(defn on-change [args]
  (tap> :reload)
  (re-render!)
  #_(reload!))

(defn start-watcher [{:keys [watch-paths]}]
  (let [watch-dir (requiring-resolve 'juxt.dirwatch/watch-dir)
        files     (->> watch-paths
                       (map #(io/file %))
                       (filter #(.exists ^java.io.File %)))]
    (apply watch-dir on-change files)))

(defn stop-watcher [watcher]
  ((requiring-resolve 'juxt.dirwatch/close-watcher) watcher))

(defn update-req [{:keys [conn] :as config} req]
  (-> req
      (merge config)
      (assoc :app/db (d/db conn))))

(defn reload-handler [config]
  (fn  [req]
    (hk-gen/->sse-response req
                           {hk-gen/on-open
                            (fn [sse-gen]
                              (let [uri  (get-in req [:query-params "uri"])
                                    page (db/get-page (:app/db req) uri)]
                                (swap! !connections assoc sse-gen {:uri    uri
                                                                   :render (fn []
                                                                             (d*/merge-fragment! sse-gen
                                                                                                 (render/render-fragment
                                                                                                  page
                                                                                                  (update-req config req))))})))

                            hk-gen/on-close
                            (fn [sse-gen _status]
                              (swap! !connections dissoc sse-gen))})))

(defn routes [config]
  ["/dev" {:post {:handler (reload-handler config)}}])

(ns site.dev
  (:require
   [clojure.java.io :as io]
   [starfederation.datastar.clojure.api :as d*]
   [starfederation.datastar.clojure.adapter.http-kit :as hk-gen]))

(def !connections (atom #{}))

(defn broadcast-fragment! [fragment]
  (doseq [c @!connections]
    (d*/merge-fragment! c fragment)))

(defn reload! []
  (doseq [c @!connections]
    (d*/execute-script! c "window.location.reload();")))

(defn on-change [args]
  (reload!))

(defn start-watcher [{:keys [watch-paths]}]
  (let [watch-dir (requiring-resolve 'juxt.dirwatch/watch-dir)
        files     (->> watch-paths
                       (map #(io/file %))
                       (filter #(.exists ^java.io.File %)))]
    (apply watch-dir on-change files)))

(defn stop-watcher [watcher]
  ((requiring-resolve 'juxt.dirwatch/close-watcher) watcher))

(defn reload-handler [req]
  (hk-gen/->sse-response req
                         {hk-gen/on-open
                          (fn [sse-gen]
                            (swap! !connections conj sse-gen))
                          hk-gen/on-close
                          (fn [sse-gen _status]
                            (swap! !connections disj sse-gen))}))

(defn routes [config]
  ["/dev" {:post {:handler reload-handler}}])

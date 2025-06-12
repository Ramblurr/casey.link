(ns site.content
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [datomic.api :as d]
   [ring.util.mime-type :as ring-mime]
   [site.class-path :as cp]
   [site.crypto :as crypto]
   [site.db :as db]
   [site.markdown :as md]))

(defn content-pattern [suffixes]
  (re-pattern (str "(" (str/join "|" suffixes) ")$")))

(defn suggest-uri [uri path]
  (or uri
      (-> (str "/" (str/replace path #"\.[^\.]+$" "/"))
          (str/replace #"index\/$" "")
          (str/replace #"\/\/+" "/"))))

(defmulti parse-file (fn [_ctx {:keys [path]}]
                       (keyword (last (str/split path #"\.")))))

(defmethod parse-file :md [{:keys [db get-page-kind]} {:keys [thunk path last-modified]}]
  (when-let [kind (get-page-kind path)]
    [(-> (md/parse-markdown-meta (thunk))
         (assoc :page/resource-path path
                :page/last-modified last-modified
                :page/kind kind)
         (update :page/uri suggest-uri path))]))

(defmethod parse-file :edn [ctx {:keys [thunk] :as r}]
  (let [data (read-string (thunk))]
    (if (and (coll? data) (not (map? data)))
      data
      [data])))

(defmethod parse-file :default [ctx _]
  nil)

(defn load-content [ctx resource]
  (try
    (let [pages (vec (parse-file (assoc ctx :db (d/db (:conn ctx)))
                                 resource))]
      (filterv :page/kind pages))
    (catch Exception e
      (let [ex {:exception e
                :path      (:path resource)
                :message   (str "Failed to parse file " (:path resource))
                :kind      ::parse-file}]
        (prn e)
        (tap> ex))
      nil)))

(defn load-all-content [ctx content-dir suffixes]
  (->> (cp/list-resources content-dir (content-pattern suffixes))
       (map (partial load-content ctx))
       (apply concat)))

(def default-content-types
  {"map"         "application/json"
   "webmanifest" "application/json"})

(def default-content-suffixes ["md" "edn"])

(def default-asset-suffixes ["css" "gif" "jpg" "jpeg" "js" "js.map" "png" "svg" "txt" "webp" "woff2" "ico" "webmanifest"])

(defn load-all-assets [content-dir suffixes]
  (->> (cp/list-resources content-dir (content-pattern suffixes))
       (map (fn [{:keys [thunk path last-modified size] :as resource}]
              (let [resource-path (cp/join content-dir path)]
                {:asset/resource-path  resource-path
                 :asset/last-modified  last-modified
                 :asset/content-type   (ring-mime/ext-mime-type resource-path default-content-types)
                 :asset/content-length size
                 :asset/hash           (crypto/sha384-resource resource-path)
                 :asset/uri            (str "/" path)})))
       (filter #(not= (:asset/resource-path %) "public/compiled.css"))))

(defn ingest-txs [conn txs]
  (d/transact conn txs))

(defn ingest! [ctx]
  (tap> :ingest!)
  (let [suffixes    default-content-suffixes
        content-dir "public/"]
    @(->> (load-all-content ctx  content-dir suffixes)
          (concat (load-all-assets content-dir default-asset-suffixes))
          (ingest-txs (:conn ctx)))
    nil))

(defn handle-request [{:app/keys [render-fn db]} req]
  (when-let [page (d/entity db
                            [:page/uri (:uri req)])]
    (render-fn page req)))

#_(defn request-handler [{:keys [conn]} render-page]
    (ingest! conn)
    (partial handle-request {:app/db        (d/db conn)
                             :app/render-fn render-page}))

(defn asset-handler [{:asset/keys [content-length last-modified content-type resource-path]}]
  (let [response {:status  200
                  :body    (io/as-file (io/resource resource-path))
                  :headers {"Content-Length" content-length
                            "Last-Modified"  last-modified
                            "Cache-Control"  "max-age=31536000,immutable,public"
                            "Content-Type"   content-type}}]
    (fn [req]
      response)))

(defn routes [{:keys [get-page-kind conn render-page] :as config}]
  (assert conn)
  (assert render-page)
  (assert get-page-kind)
  (ingest! config)
  (let [db     (d/db conn)
        pages  (db/get-pages db)
        assets (db/get-assets db)]
    [""
     (mapv (fn [page]
             [(:page/uri page)
              {:handler               #(render-page page (merge config %))
               :sitemap/last-modified (:page/last-modified page)}])
           pages)
     [""
      (mapv (fn [asset]
              [(:asset/uri asset)
               {:handler          (asset-handler asset)
                :sitemap/exclude? true}])
            assets)]]))

(comment
  (:content
   (parse-post "public/blog/fairybox/index.md"))
  (format-date "2023-01-12")
  (article-index-data)
  (article-route-data)
  (cp/slurp-resources "public/blog" #"\.md$")
  (cp/list-resources "public/blog" #".*\.(jpg|jpeg|gif|webp|png|svg)$")
  (cp/slurp-resources "public/" #".*\.(jpg|jpeg|gif|webp|png|svg)$")
  (cp/slurp-resources "public/blog" #".*\.(jpg|jpeg|gif|webp|png|svg)$")

  :assets (cp/list-resources "public/" #".*\.(css|gif|jpg|jpeg|js|map|png|svg|txt|webp|woff2)$")

  ((->
    (cp/list-resources "public/blog" #"\.md$")
    (first)
    :thunk))

  (ring-mime/ext-mime-type "public/js/datastar@1.0.0-RC.11.js.map"  {"map" "application/json"})

  (do
    (require '[site.pages :as pages])
    (def _conn (db/create-database "datomic:mem://site-dev"))
    (ingest! {:conn          _conn
              :get-page-kind pages/get-page-kind})
    (def _db (d/db _conn)))
  (d/release _conn)
  (->>
   (d/q '[:find (pull ?e [*])
          :in $
          :where [?e :asset/uri _]]
        _db)
   (map first)
   (map :asset/uri))

  (get-pages _db)

  (d/entity _db [:db/ident :page/uri])
  (d/entity _db [:page/uri "/blog"])
  (map :asset/uri
       (load-all-assets "public/" default-asset-suffixes))
  (load-all-content _conn "public/" default-content-suffixes)
  (db/get-pages _db)
  (db/get-page _db "/blog")
  ;; rcf
  )

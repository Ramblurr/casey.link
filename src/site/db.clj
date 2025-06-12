(ns site.db
  (:require
   [datomic.api :as d]))

(defn schema->tx-data
  [schema]
  (reduce (fn [schema [attr-id attr-map]]
            (conj schema (assoc attr-map :db/ident attr-id))) [] schema))

(def schema
  (schema->tx-data
   (merge
    #:page {:kind          {:db/valueType   :db.type/keyword
                            :db/cardinality :db.cardinality/one}
            :uri           {:db/valueType   :db.type/string
                            :db/unique      :db.unique/identity
                            :db/cardinality :db.cardinality/one}
            :title         {:db/valueType   :db.type/string
                            :db/cardinality :db.cardinality/one}
            :resource-path {:db/valueType   :db.type/string
                            :db/cardinality :db.cardinality/one}
            :last-modified {:db/valueType   :db.type/long
                            :db/cardinality :db.cardinality/one}
            :body          {:db/valueType   :db.type/string
                            :db/cardinality :db.cardinality/one}}

    #:asset {:uri            {:db/valueType   :db.type/string
                              :db/unique      :db.unique/identity
                              :db/cardinality :db.cardinality/one}
             :resource-path  {:db/valueType   :db.type/string
                              :db/cardinality :db.cardinality/one}
             :content-type   {:db/valueType   :db.type/string
                              :db/cardinality :db.cardinality/one}
             :last-modified  {:db/valueType   :db.type/long
                              :db/cardinality :db.cardinality/one}
             :content-length {:db/valueType   :db.type/long
                              :db/cardinality :db.cardinality/one}
             :hash           {:db/valueType   :db.type/string
                              :db/cardinality :db.cardinality/one}}
    #:blog {:author      {:db/valueType   :db.type/string
                          :db/cardinality :db.cardinality/one}
            :description {:db/valueType   :db.type/string
                          :db/cardinality :db.cardinality/one}
            :tags        {:db/valueType   :db.type/string
                          :db/cardinality :db.cardinality/many}
            :date        {:db/valueType   :db.type/string
                          :db/cardinality :db.cardinality/one}
            :modified    {:db/valueType   :db.type/string
                          :db/cardinality :db.cardinality/one}})))

(defn create-database [uri]
  (d/create-database uri)
  (let [conn (d/connect uri)]
    @(d/transact conn schema)
    conn))

(defn close [conn]
  (d/release conn)
  (d/shutdown false))

(defn get-attr [db attr]
  (d/entity db [:db/ident attr]))

(defn wrap-datomic [handler {:keys [conn]}]
  (fn [req]
    (handler (assoc req :app/db (d/db conn)))))

(defn find-all-by
  [db attr attr-val]
  (d/q '[:find [?e ...]
         :in $ ?attr ?val
         :where [?e ?attr ?val]]
       db attr attr-val))

(defn find-all
  "Returns a list of all entities having attr"
  [db attr]
  (d/q '[:find [?e ...]
         :in $ ?attr
         :where
         [?e ?attr ?v]]
       db attr))

(defn find-by
  "Returns the unique entity identified by attr and val."
  [db attr attr-val]
  (first (find-all-by db attr attr-val)))

(defn get-blog-posts [db]
  (assert db)
  (->> (find-all db :blog/author)
       (map #(d/entity db %))
       (sort-by :blog/date)
       (reverse)))

(defn get-pages [db]
  (assert db)
  (->> (find-all db :page/uri)
       (map #(d/entity db %))))

(defn get-assets [db]
  (assert db)
  (->> (find-all db :asset/uri)
       (map #(d/entity db %))))

(defn get-page [db uri]
  (assert db)
  (->> (find-by db :page/uri uri)
       (d/entity db)))

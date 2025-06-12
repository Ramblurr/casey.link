(ns site.class-path
  (:require

   [clojure.java.io :as io]
   [clojure.string :as str])
  (:import [java.nio.file FileSystems Files Path Paths]

           [java.net URI]))

(defn class-path-elements []
  (->> (str/split (System/getProperty "java.class.path" ".") #":")
       (remove #(.contains % "/.m2/"))))

(defn walk-files
  "Returns a seq of maps with :path and :last-modified for all files under root"
  [^Path root]
  (->> (.iterator (Files/walk root (make-array java.nio.file.FileVisitOption 0)))
       iterator-seq
       (filter #(Files/isRegularFile % (make-array java.nio.file.LinkOption 0)))
       (map (fn [^Path path]
              {:path          (str (.relativize root path))
               :last-modified (.toMillis (Files/getLastModifiedTime path (make-array java.nio.file.LinkOption 0)))
               :size          (Files/size path)}))))

(defn get-resource-metadata [path-str]
  (let [file (java.io.File. path-str)]
    (cond
      (.isDirectory file)
      (walk-files (Paths/get path-str (make-array String 0)))

      (str/ends-with? path-str ".jar")
      (with-open [fs (FileSystems/newFileSystem
                      (URI/create (str "jar:" (.toURI file)))
                      {})]
        (vec (walk-files (.getPath fs "/" (make-array String 0)))))

      :else [])))

(defn just-the-filename [^String path]
  (last (str/split path #"/")))

(defn with-trailiing-slash [^String s]
  (if (str/ends-with? s "/")
    s
    (str s "/")))

(defn join
  "Joins path parts into a single path string"
  [& parts]
  (str (Paths/get (first parts) (into-array String (rest parts)))))

(defn chop-up-to [^String prefix ^String s]
  (subs s (+ (.indexOf s prefix)
             (count prefix))))

(defn file-metadata-on-class-path []
  (->> (class-path-elements)
       (mapcat get-resource-metadata)))

(defn slurp-resources
  "Returns a map of paths to file contents in the `dir` found on the resource
  path. `regexp` will be used to filter the files. `opts` are passed to `slurp`
  to enable specification of encoding and buffer-size etc."
  [dir regexp & opts]
  (let [dir (with-trailiing-slash dir)]
    (->> (file-metadata-on-class-path)
         (map :path)
         (filter (fn [^String s] (.contains s dir)))
         (filter #(re-find regexp %))
         (map (juxt #(chop-up-to dir %)
                    #(apply slurp (io/resource %) opts)))
         (into {}))))

(defn list-resources
  "Returns a vector of {:path :last-modified :thunk} of files in the `dir` found on the resource
  path whose name matches `regexp`."
  [dir regexp & opts]
  (let [dir (with-trailiing-slash dir)]
    (->> (file-metadata-on-class-path)
         (filter (fn [{:keys [path]}] (.contains ^String path dir)))
         (filter (fn [{:keys [path]}] (re-find regexp ^String path)))
         (map (fn [e]
                (-> e
                    (update  :path
                             #(chop-up-to dir %))
                    (assoc  :thunk (fn []
                                     (apply slurp (io/resource (:path e)) opts))))))

         (into []))))

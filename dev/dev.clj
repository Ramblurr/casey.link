(ns dev
  {:clj-kondo/config '{:linters {:unused-namespace     {:level :off}
                                 :unresolved-namespace {:level :off}
                                 :unused-referred-var  {:level :off}}}}
  (:require
   [site.server :as server]
   [donut.system.repl :as dsr]
   [clj-reload.core :as clj-reload]))

(defn start []
  (dsr/start ::server/dev))

(defn stop []
  (dsr/stop))

(defn restart []
  (stop)
  (start))

(defn reset []
  (clj-reload/reload))

(defn reset-all []
  (clj-reload/reload {:only :all}))

(clj-reload/init {:dirs        ["src" "dev" "test"]
                  :no-reload   #{'user}
                  :unload-hook 'stop
                  :reload-hook 'start})
(comment
  ;; Start your system
  (start)
  ;; Stop the system (if it is started)
  (stop)
  ;; Restart the system =  stop + start
  (restart)
  ;; rcf
  ;; Reset = stop + hot reset all clojure code + start
  (reset)
  (reset-all)
  1
  (clojure.repl.deps/sync-deps)
  ;;
  )

(ns user
  {:clj-kondo/config '{:linters {:unused-namespace     {:level :off}
                                 :unresolved-namespace {:level :off}
                                 :unused-referred-var  {:level :off}}}})

(defn dev
  "Load and switch to the 'dev' namespace."
  []
  (require 'dev)
  (in-ns 'dev))

(dev)

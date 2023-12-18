(ns mycc.dev
  (:require
    [mycc.core] ;; so it gets loaded
    [mycc.base.core :as base]
    [mycc.seed :as seed]))

(defn start! []
  (base/start!))

(defn stop! []
  (base/stop!))

#_(start!)
#_(seed/seed!)

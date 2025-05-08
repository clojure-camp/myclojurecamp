(ns mycc.dev
  (:require
    [hyperfiddle.rcf]
    [mycc.core] ;; so it gets loaded
    [mycc.base.core :as base]
    [mycc.seed :as seed]))

(hyperfiddle.rcf/enable!)

(defn start! []
  (base/start!))

(defn stop! []
  (base/stop!))

#_(start!)
#_(seed/seed!)

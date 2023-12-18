(ns mycc.core
  (:gen-class)
  (:require
    [mycc.p2p.core] ;; to load module
    [mycc.base.core :as base]))

(defn -main []
  (base/start!))

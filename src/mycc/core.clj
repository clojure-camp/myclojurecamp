(ns mycc.core
  (:gen-class)
  (:require
    [mycc.base.core :as base]
    ;; modules
    [mycc.p2p.core]
    [mycc.profile.core]))

(defn -main []
  (base/start!))



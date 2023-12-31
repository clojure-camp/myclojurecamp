(ns mycc.core
  (:gen-class)
  (:require
    [mycc.base.core :as base]))

(defn -main []
  (base/start!))



(ns mycc.dev
  (:require
    [hyperfiddle.rcf]
    [modulo.config :as config]
    [mycc.core] ;; so it gets loaded
    [mycc.base.core :as base]
    [mycc.seed :as seed]))

(hyperfiddle.rcf/enable!)

(defn start! []
  (base/start!))

(defn stop! []
  (base/stop!))

(defn reload-config! []
  (config/initialize!
   base/config-schema
   base/config-default))

#_(start!)
#_(seed/seed!)

#_(reload-config!)


(ns mycc.base.client.core
  (:require
    [modulo.api :as mod]
    [mycc.base.client.ui.app :as app]
    [mycc.modules] ;; to register modules
    ))

(defn ^:export init []
  (mod/initialize! app/app-view))

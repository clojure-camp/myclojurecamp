(ns mycc.base.client.core
  (:require
    [modulo.api :as mod]
    [mycc.base.client.ui.app :as app]
    ;; modules:
    [mycc.profile.core]
    [mycc.p2p.core]))

(defn init []
  (mod/initialize! app/app-view))

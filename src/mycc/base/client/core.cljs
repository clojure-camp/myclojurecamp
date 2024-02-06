(ns mycc.base.client.core
  (:require
    [modulo.api :as mod]
    [mycc.base.client.ui.app :as app]
    ;; modules:
    [mycc.profile.core]
    [mycc.p2p.core]
    [mycc.studygroup.core]))

(defn ^:export init []
  (mod/initialize! app/app-view))

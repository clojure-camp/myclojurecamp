(ns mycc.stats.ui
  (:require
    [modulo.api :as mod]
    [mycc.common.ui :as ui]))

;; we're computing the HTML server side
;; to not leak user info unnecessarily
(defn stats-page-view []
  [ui/server-html-view
   {:route "/api/stats/all"}])

(mod/register-page!
  {:page/id :page.id/stats
   :page/path "/stats"
   ;; hide from menu, not for security reasons, just for UX
   ;; :page/nav-label "stats"
   :page/view #'stats-page-view
   :page/on-enter! (fn [])})

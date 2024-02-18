(ns mycc.stats.ui
  (:require
    [modulo.api :as mod]
    [reagent.core :as r]
    [bloom.commons.ajax :as ajax]))

;; we're computing the HTML server side
;; to not leak user info unnecessarily
(defn stats-page-view []
  (r/with-let
    [data (r/atom nil)
     _ (when (nil? @data)
         (ajax/request {:method :get
                        :uri "/api/stats/all"
                        :on-success (fn [d]
                                      (reset! data d))}))]
    [:div {:dangerouslySetInnerHTML {:__html (:content @data)}}]))

(mod/register-page!
  {:page/id :page.id/stats
   :page/path "/stats"
   ;; hide from menu, not for security reasons, just for UX
   :page/nav-label "stats"
   :page/view #'stats-page-view
   :page/on-enter! (fn [])})

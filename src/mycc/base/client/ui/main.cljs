(ns mycc.base.client.ui.main
  (:require
    [re-frame.core :refer [dispatch]]
    [bloom.commons.fontawesome :as fa]
    [mycc.base.client.ui.debug :as debug]
    [mycc.base.client.state :as state]
    [modulo.api :as mod]))

(defn popover-view
  [content]
  [:div.info
   [fa/fa-question-circle-solid]
   [:div.popover
    content]])

(defn ajax-status-view []
  [:div.ajax-status {:class (if (empty? @state/ajax-state) "normal" "loading")}
   (if (empty? @state/ajax-state)
     [fa/fa-check-circle-solid]
     [fa/fa-circle-notch-solid])])

(defn header-view []
  [:div.header
   [:img.logomark
    {:src "/logomark.svg"
     :alt "Logo of Clojure Camp. A star constellation in the shape of alambda."}]
   [:div.gap]
   [:img.logotype
    {:src "/logotype.svg"
     :alt "Clojure Camp"}]
   [:div.gap]
   [:button.log-out
    {:on-click (fn []
                 (dispatch [:log-out!]))}
    "Log Out"]])

(defn nav-view []
  [:div.nav
   [:div.items
    (doall
      (for [page (vals @(mod/pages))
            :when (:page/nav-label page)]
        ^{:key (:page/id page)}
        [:a {:href (mod/path-for [(:page/id page)])
             :class (when (mod/active? [(:page/id page)])
                      "active")}
         (:page/nav-label page)]))]])

(defn main-view []
  [:div.main
   [ajax-status-view]
   [header-view]
   [nav-view]
   [:div.content
    [mod/current-page-view]]
   (when debug/debug?
     [debug/db-view])])

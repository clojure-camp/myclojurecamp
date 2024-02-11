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
  (let [saved? (empty? @state/ajax-state)]
    [:div.ajax-status
     {:tw "pointer-events-none fixed inset-x-0 bottom-0.5em flex justify-center"
      :style (when saved?
               {:animation "fade-out 2s forwards ease-in-out"})}
     [:div {:tw "text-white bg-clojure-camp-blue-lighter rounded-full p-1 pr-3 flex gap-1 items-center"}
      (if saved?
        [:<>
         [fa/fa-check-circle-solid
          {:tw "w-1em h-1em"}]
         "Saved"]
        [fa/fa-circle-notch-solid
         {:style {:animation "spin 1s infinite linear"}
          :tw "w-1em h-1em"}])]]))

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
    {:tw "cursor-pointer"
     :on-click (fn []
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

(ns ^:figwheel-hooks
  mycc.base.client.core
  (:require
   [re-frame.core :refer [dispatch-sync]]
   [reagent.dom :as rdom]
   [mycc.base.client.ui.app :as app]
   [mycc.base.client.pages :as pages]
   ;; modules:
   [mycc.profile.core]
   [mycc.p2p.core]))

(defn render []
  (rdom/render
   [app/app-view]
   (js/document.getElementById "app")))

(defn ^:export init []
  (dispatch-sync [:initialize!])
  (pages/initialize!)
  (render))

(defn ^:after-load reload
  []
  (render))

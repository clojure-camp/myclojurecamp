(ns ^:figwheel-hooks
  modulo.client.core
  (:require
   [re-frame.core :refer [dispatch-sync]]
   [reagent.dom :as rdom]
   [modulo.client.pages :as pages]))

(defonce root-view (atom [:div]))

(defn render []
  (rdom/render
   [@root-view]
   (js/document.getElementById "app")))

(defn init []
  (dispatch-sync [:initialize!])
  (pages/initialize!)
  (render))

(defn ^:after-load reload
  []
  (render))

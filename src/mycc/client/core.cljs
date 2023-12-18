(ns ^:figwheel-hooks
  mycc.client.core
  (:require
   [re-frame.core :refer [dispatch]]
   [reagent.dom :as rdom]
   [mycc.client.ui.app :as app]))

(defn render []
  (rdom/render
   [app/app-view]
   (js/document.getElementById "app")))

(defn ^:export init []
  (dispatch [:initialize!])
  (render))

(defn ^:after-load reload
  []
  (render))

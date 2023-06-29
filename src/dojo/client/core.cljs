(ns ^:figwheel-hooks
  dojo.client.core
  (:require
   [re-frame.core :refer [dispatch]]
   [reagent.dom :as rdom]
   [dojo.client.ui.app :as app]))

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

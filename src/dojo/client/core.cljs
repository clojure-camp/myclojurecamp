(ns ^:figwheel-hooks
  dojo.client.core
  (:require
    [reagent.dom :as rdom]))

(defn app-view []
  [:div "hello world"])

(defn render []
  (rdom/render
    [app-view]
    (js/document.getElementById "app")))

(defn ^:export init []
  (render))

(defn ^:after-load reload
  []
  (render))

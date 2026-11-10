(ns ^:figwheel-hooks
  modulo.client.core
  (:require
   [bloom.omni.reagent :as rdom]
   [modulo.client.pages :as pages]))

(defonce root-view (atom [:div]))

(defn render []
  (rdom/render [@root-view]))

(defn init []
  (pages/initialize!)
  (render))

(defn ^:after-load reload
  []
  (render))

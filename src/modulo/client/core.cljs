(ns ^:figwheel-hooks
  modulo.client.core
  (:require
   [bloom.omni.reagent :as rdom]
   [re-frame.core :refer [dispatch-sync]]
   [modulo.client.pages :as pages]))

(defonce root-view (atom [:div]))

(defn render []
  (rdom/render [@root-view]))

(defn init []
  (dispatch-sync [:initialize!])
  (pages/initialize!)
  (render))

(defn ^:after-load reload
  []
  (render))

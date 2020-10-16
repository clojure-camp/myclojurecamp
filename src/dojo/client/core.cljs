(ns ^:figwheel-hooks
  dojo.client.core
  (:require
    [clojure.string :as string]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.dom :as rdom]
    [reagent.core :as r]
    [dojo.client.state]
    [dojo.model :as model]))

(defn availability-view []
  (when-let [availability (:user/availability @(subscribe [:user]))]
    [:table
     [:thead
      [:tr
       [:th]
       (for [day model/days]
         ^{:key day}
         [:th (string/capitalize (name day))])]]
     [:tbody
      (doall
        (for [hour model/hours]
          ^{:key hour}
          [:tr
           [:td
            hour]
           (doall
             (for [day model/days]
               ^{:key day}
               [:td
                (let [value (availability [day hour])]
                  [:button {:on-click (fn [_]
                                        (dispatch [:set-availability!
                                                   [day hour]
                                                   (case value
                                                     :preferred nil
                                                     :available :preferred
                                                     nil :available)]))}
                   (case value
                     :preferred "P"
                     :available "A"
                     nil "")])]))]))]]))

(defn app-view []
  [:div
   [:pre (str @(subscribe [:user]))]
   [:label
    [:input {:type "checkbox"}]
    "Pair this week?"]
   [availability-view]])

(defn render []
  (rdom/render
    [app-view]
    (js/document.getElementById "app")))

(defn ^:export init []
  (dispatch [:initialize!])
  (render))

(defn ^:after-load reload
  []
  (render))

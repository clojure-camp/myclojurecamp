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

(defn login-view []
  [:form.login
   {:on-submit (fn [e]
                 (.preventDefault e)
                 (dispatch [:log-in! (.. e -target -elements -email -value)]))}
   [:label
    "Enter your email:"
    [:input {:name "email"
             :type "email"}]
    [:button "Go"]]])

(defn main-view []
  [:div.main
   [:label
    [:input {:type "checkbox"}]
    "Pair this week?"]
   [availability-view]])

(defn app-view []
  [:<>
   (if-let [user @(subscribe [:user])]
     [main-view]
     [login-view])])

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

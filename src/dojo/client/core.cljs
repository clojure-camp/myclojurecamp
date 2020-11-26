(ns ^:figwheel-hooks
  dojo.client.core
  (:require
    [clojure.string :as string]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.dom :as rdom]
    [reagent.core :as r]
    [dojo.client.state]
    [dojo.model :as model]))

(defn topics-view []
  [:div.topics-view
   (let [user-topic-ids @(subscribe [:user-topic-ids])]
     (for [topic @(subscribe [:topics])
           :let [checked? (contains? user-topic-ids (:topic/id topic))]]
       ^{:key (:topic/id topic)}
       [:div.topic
        [:label
         [:input {:type "checkbox"
                  :checked checked?
                  :on-change
                  (fn []
                    (if checked?
                      (dispatch [:remove-user-topic! (:topic/id topic)])
                      (dispatch [:add-user-topic! (:topic/id topic)])))}]
         [:span.name (:topic/name topic)] " "
         [:span.count (:topic/user-count topic)]]]))
   [:button
    {:on-click (fn [_]
                 (let [value (js/prompt "Enter a new topic:")]
                   (when (not (string/blank? value))
                     (dispatch [:new-topic! (string/trim value)]))))}
    "+"]])

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
  (let [sent-email (r/atom nil)]
    (fn []
      [:form.login
       {:on-submit (fn [e]
                     (let [email (.. e -target -elements -email -value)]
                       (.preventDefault e)
                       (dispatch [:log-in! email])
                       (reset! sent-email email)))}
       [:label
        "Enter your email:"
        [:input {:name "email"
                 :type "email"}]]
       [:button "Go"]
       (when @sent-email
         [:div "An email with a login-link was sent to " @sent-email])])))

(defn main-view []
  [:div.main
   [:button {:on-click (fn []
                         (dispatch [:log-out!]))} "Log Out"]
   [:label
    [:input {:type "checkbox"}]
    "Pair this week?"]
   [topics-view]
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

(ns ^:figwheel-hooks
 dojo.client.core
  (:require
   [clojure.string :as string]
   [garden.core :as garden]
   [re-frame.core :refer [dispatch subscribe]]
   [reagent.dom :as rdom]
   [reagent.core :as r]
   [bloom.commons.fontawesome :as fa]
   [dojo.client.state]
   [dojo.client.styles :refer [styles]]
   [dojo.model :as model]))

(defn max-limit-preferences-view []
  [:div.max-limit-preferences
   [:label
    "Max pair per day"
    [:input {:type "number"
             :value @(subscribe [:user-profile-value :user/max-pair-per-day])
             :min 0
             :max 24
             :on-change (fn [e]
                           (dispatch [:set-user-value!
                                      :user/max-pair-per-day
                                      (js/parseInt (.. e -target -value) 10)]))}]]
   [:label
    "Max pair per week"
    [:input {:type "number"
             :value @(subscribe [:user-profile-value :user/max-pair-per-week])
             :min 0
             :max (* 24 7)
             :on-change (fn [e]
                           (dispatch [:set-user-value!
                                      :user/max-pair-per-week
                                      (js/parseInt (.. e -target -value) 10)]))}]]])
(defn next-day-of-week
  "Calculates next date with day of week as given"
  [now target-day-of-week]
  (let [target-day-of-week ({:monday 1
                             :tuesday 2
                             :wednesday 3
                             :thursday 4
                             :friday 5
                             :saturday 6
                             :sunday 0} target-day-of-week)
        now-day-of-week (.getDay now)
        ;; must be a nice way to do this with mod
        ;; (mod (- 7 now-day-of-week) 7)
        delta-days (get (zipmap (range 0 7)
                                (take 7 (drop (- 7 target-day-of-week)
                                              (cycle (range 7 0 -1)))))
                        now-day-of-week)
        new-date (doto (js/Date. (.valueOf now))
                   (.setDate (+ delta-days (.getDate now))))]
    new-date))

(defn add-days [day delta-days]
  (doto (js/Date. (.valueOf day))
    (.setDate (+ delta-days (.getDate day)))))

(defn format-date [date]
  (.format (js/Intl.DateTimeFormat. "en-US" #js {:weekday "long"
                                                 :month "short"
                                                 :day "numeric"})
           date))

(defn topics-view []
  [:div.topics-view
   [:h1 "Topics"]
   (let [user-topic-ids @(subscribe [:user-profile-value :user/topic-ids])]
     [:div.topics
      (for [topic (sort-by :topic/name @(subscribe [:topics]))
            :let [checked? (contains? user-topic-ids (:topic/id topic))]]
        ^{:key (:topic/id topic)}
        [:label.topic
         [:input {:type "checkbox"
                  :checked checked?
                  :on-change
                  (fn []
                    (if checked?
                      (dispatch [:remove-user-topic! (:topic/id topic)])
                      (dispatch [:add-user-topic! (:topic/id topic)])))}]
         [:span.name (:topic/name topic)] " "
         [:span.count (:topic/user-count topic)]])])
   [:button
    {:on-click (fn [_]
                 (let [value (js/prompt "Enter a new topic:")]
                   (when (not (string/blank? value))
                     (dispatch [:new-topic! (string/trim value)]))))}
    "+ Add Topic"]])

(defn availability-view []
  (when-let [availability @(subscribe [:user-profile-value :user/availability])]
    [:table.availability
     [:thead
      [:tr
       [:th]
       (let [next-monday (next-day-of-week (js/Date.) :monday)]
         (for [[i day] (map-indexed (fn [i d] [i d]) model/days)]
          (let [[day-of-week date] (string/split (format-date (add-days next-monday i)) #",")]
           ^{:key day}
           [:th.day
            [:div.day-of-week day-of-week]
            [:div.date date]])))]]
     [:tbody
      (doall
       (for [hour model/hours]
         ^{:key hour}
         [:tr
          [:td.hour
           hour]
          (doall
           (for [day model/days]
             ^{:key day}
             [:td
              (let [value (availability [day hour])]
                [:button
                 {:class (case value
                           :preferred "preferred"
                           :available "available"
                           nil "empty")
                  :on-click (fn [_]
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

(defn opt-in-view []
  (let [checked? @(subscribe [:user-profile-value :user/pair-next-week?])]
   [:button.opt-in
    {:class (when checked? "active")
     :on-click (fn []
                 (dispatch
                  [:opt-in-for-pairing! (not checked?)]))}
    (if checked?
      [fa/fa-check-square-regular]
      [fa/fa-square-regular])
    "Pair next week?"]))

(defn name-view []
  [:label "Name "
   [:input {:type "text"
            :value @(subscribe [:user-profile-value :user/name])
            :on-change (fn [e]
                          (dispatch
                            [:set-user-value! :user/name (.. e -target -value)]))}]])

(defn time-zone-view []
  [:label "Time Zone "
   [:input {:type "text"
            :disabled true
            :value @(subscribe [:user-profile-value :user/time-zone])}]
   [:button
    {:on-click (fn []
                  (dispatch
                    [:set-user-value! :user/time-zone (.. js/Intl DateTimeFormat resolvedOptions -timeZone)]))}
    "Update"]])

(defn main-view []
  [:div.main
   [:button.log-out
    {:on-click (fn []
                 (dispatch [:log-out!]))}
    "Log Out"]
   [opt-in-view]
   [name-view]
   [topics-view]
   [max-limit-preferences-view]
   [time-zone-view]
   [availability-view]])

(defn app-view []
  [:<>
   [:style
    (garden/css styles)]

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

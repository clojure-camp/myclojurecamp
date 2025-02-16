(ns mycc.p2p.ui
  (:require
    [clojure.string :as string]
    [bloom.commons.fontawesome :as fa]
    [modulo.api :as mod]
    [reagent.core :as r]
    [mycc.common.profile :as common.profile]
    [mycc.common.ui :as ui]
    [mycc.p2p.util :as util]
    [mycc.p2p.styles :as styles]))

(defn popover-view
  [content]
  [:div.info
   [fa/fa-question-circle-solid]
   [:div.popover
    content]])

(defn max-limit-preferences-view []
  [:<>
   [ui/row
    {:title "Max pairing sessions per day"}
    [ui/input {:type "number"
               :value @(mod/subscribe [:user-profile-value :user/max-pair-per-day])
               :min 1
               :max 24
               :on-change (fn [e]
                            (mod/dispatch [:set-user-value!
                                           :user/max-pair-per-day
                                           (js/parseInt (.. e -target -value) 10)]))}]]
   [ui/row
    {:title "Max pairing sessions per week"}
    [ui/input {:type "number"
               :value @(mod/subscribe [:user-profile-value :user/max-pair-per-week])
               :min 1
               :max (* 24 7)
               :on-change (fn [e]
                            (mod/dispatch [:set-user-value!
                                           :user/max-pair-per-week
                                           (js/parseInt (.. e -target -value) 10)]))}]]

   [ui/row
    {:title "Max pairing sessions with same person in a week"
     :info "The algorithm will try to maximize the variety of partners you pair with, but may still pair you several times with the same person. This setting lets you limit the maximum number of times you will be scheduled with the same person in a given week."}
    [ui/input {:type "number"
               :value @(mod/subscribe [:user-profile-value :user/max-pair-same-user])
               :min 1
               :max 50
               :on-change (fn [e]
                            (mod/dispatch [:set-user-value!
                                           :user/max-pair-same-user
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
  (.format (js/Intl.DateTimeFormat. "en-US" #js {:weekday "short"
                                                 :month "short"
                                                 :day "numeric"})
           date))

(defn pair-with-view []
  (when (= :role/student @(mod/subscribe [:user-profile-value :user/role]))
    [ui/row
     {:title "Pair with..."}
     [ui/radio-list
      {:choices [[:pair-with/only-mentors "Mentors Only"]
                 [:pair-with/prefer-mentors "Mentors Preferred"]
                 [nil "No Preference"]
                 [:pair-with/prefer-students "Students Preferred"]
                 [:pair-with/only-students "Students Only"]]
       :value @(mod/subscribe [:user-profile-value :user/pair-with])
       :direction :vertical
       :on-change (fn [value]
                    (mod/dispatch [:set-user-value! :user/pair-with value]))}]]))

(defn hour-rows-view
  [availability hours]
  [:tbody
   (for [hour hours]
     ^{:key hour}
     [:tr
      [:td.hour hour ":00"]
      (doall
        (for [day util/days]
          ^{:key day}
          [:td
           (let [value (availability [day hour])]
             [:button
              {:class (case value
                        :preferred "preferred"
                        :available "available"
                        nil "empty")
               :on-click (fn [_]
                           (mod/dispatch [:set-availability!
                                          [day hour]
                                          (case value
                                            :preferred nil
                                            :available :preferred
                                            nil :available)]))}
              [:div.wrapper
               (case value
                 :preferred "P"
                 :available "A"
                 nil "")]])]))])])

(defn availability-view []
  (r/with-let [force-show-early? (r/atom false)
               force-show-late? (r/atom false)]
    [ui/row
     {:title "Availability"
      :info [:<>
             [:div "Click in the calendar grid below to indicate your time availability."]
             [:div "A = available, P = preferred"]]}
     [:<>
      [:div {:tw "absolute right-4 top-4 flex gap-4 items-center"}
       [ui/secondary-button {:on-click (fn [_]
                                         (mod/dispatch [:clear-availability!]))} "Clear all"]
       #_[:div {:tw "flex gap-1 items-center"}
        [fa/fa-globe-solid {:tw "w-4 h-4"}]
        @(mod/subscribe [:user-profile-value :user/time-zone])]]
      (when-let [availability @(mod/subscribe [:user-profile-value :user/availability])]
        [:div {:tw "max-h-100vh overflow-x-auto mt-4"}
         [:table.availability
          [:thead
           [:tr
            [:th {:tw "sticky top-0 bg-white z-100"}]
            (let [next-monday (next-day-of-week (js/Date.) :monday)]
              (for [[i day] (map-indexed (fn [i d] [i d]) util/days)]
                (let [[day-of-week date] (string/split (format-date (add-days next-monday i)) #",")]
                  ^{:key day}
                  [:th.day {:tw "sticky top-0 bg-white"}
                   [:div.day-of-week day-of-week]
                   [:div.date date]])))]]
          (if (or @force-show-early?
                  (some availability (for [hour util/early-hours
                                           day util/days]
                                       [day hour])))
            [:<>
             [:tbody
              [:tr ;; filler row, so that header doesn't cover the offset hour label
               [:td {:col-span 8 :tw "h-1em"} ""]]]
             [hour-rows-view availability util/early-hours]]
            [:tbody
             [:tr
              [:td]
              [:td {:col-span 7}
               [:div {:tw "flex justify-center"}
                [ui/secondary-button {:on-click (fn []
                                                  (reset! force-show-early? true))}
                 "Show earlier hours"]]]]])
          [hour-rows-view availability util/hours]
          (if (or @force-show-late?
                  (some availability (for [hour util/late-hours
                                           day util/days]
                                       [day hour])))
            [hour-rows-view availability util/late-hours]
            [:tbody
             [:tr
              [:td]
              [:td {:col-span 7}
               [:div {:tw "flex justify-center"}
                [ui/secondary-button {:on-click (fn []
                                                  (reset! force-show-late? true))}
                 "Show later hours"]]]]])]])]]))

(defn warning-view
  []
  (let [validations {"Add at least one hour of Availability"
                     (->> @(mod/subscribe [:user-profile-value :user/availability])
                          (some val)
                          boolean)
                     "Add a Role"
                     (boolean @(mod/subscribe [:user-profile-value :user/role]))
                     "Add a Primary Language"
                     (boolean (seq @(mod/subscribe [:user-profile-value :user/primary-languages])))
                     "Select at least one Topic"
                     (->> @(mod/subscribe [:user-profile-value :user/topics])
                          (some val)
                          boolean)}
        error-messages (->> validations
                            (remove (fn [[_ v]]
                                      v))
                            (map key))]
    (when (and
           @(mod/subscribe [:user-profile-value :user/pair-next-week?])
           (seq error-messages))
      [ui/row {}
       [:div {:tw "bg-red-100 text-red-900 p-4 rounded border border-red-200"}
        "Your current configuration means you won't get matched with anyone this week:"
        [:ul {:tw "list-disc pl-8"}
         (for [e error-messages]
           ^{:key e}
           [:li {:tw "my-2"} e])]]])))

(defn opt-in-view []
  [ui/row
   {:title "Opt-in for pairing next week?"
    :info [:div
           [:div "Do you want to participate this week?"
            [:div "(You need to opt-in every week)"]]]
    :featured? true}
   [ui/radio-list
    {:choices [[true "Yes"]
               [false "No"]]
     :value @(mod/subscribe [:user-profile-value :user/pair-next-week?])
     :on-change (fn [value]
                  (mod/dispatch [:opt-in-for-pairing! value]))}]])

(defn subscription-toggle-view []
  [ui/row
   {:title "Participate in P2P Pairing?"
    :info "Set to No to stop receiving weekly opt-in emails."}
   [ui/radio-list
    {:choices [[true "Yes"]
               [false "No"]]
     :value @(mod/subscribe [:user-profile-value :user/subscribed?])
     :on-change (fn [value]
                  (mod/dispatch [:set-user-value! :user/subscribed? value]))}]])

(defn format-date-2 [date]
  (.format (js/Intl.DateTimeFormat. "default" #js {:day "numeric"
                                                   :month "short"
                                                   :year "numeric"
                                                   :hour "numeric"})
           date))

(defn event-view [heading event]
  (let [guest-name (:user/name (:event/other-guest event))
        guest-user-id (:user/id (:event/other-guest event))]
    [:tr.event {:class (if (< (js/Date.) (:event/at event))
                         "future"
                         "past")}
     [:th heading]
     [:td
      [:span.at (format-date-2 (:event/at event))]
      " with "
      [:span.other-guest (:user/name (:event/other-guest event))]]
     [:td
      [:div.actions
       [:a.link {:href (str "mailto:" (:user/email (:event/other-guest event)))}
        [fa/fa-envelope-solid]]
       #_[:a.link {:href (util/->event-url event)}
        [fa/fa-video-solid]]


       (let [avoiding? (contains? (:user/user-pair-deny-list @(mod/subscribe [:user]))
                                  guest-user-id)]
         [:button.avoid
          {:tw ["p-1" (when avoiding? "text-white bg-red-800 rounded-full")]
           :title "Don't pair me with this user in the future."
           :on-click (fn []
                       (mod/dispatch
                         [:avoid-user! guest-user-id (not avoiding?)]))}
          [fa/fa-ban-solid {:tw "w-4 h-4"}]])]]]))

(defn events-view []
  [ui/row
   {}
   (let [events @(mod/subscribe [:events])
         [upcoming-events past-events] (->> events
                                            (sort-by :event/at)
                                            reverse
                                            (split-with (fn [event]
                                                          (< (js/Date.) (:event/at event)))))]
     [:table.events
      [:tbody
       (for [[index event] (map-indexed vector upcoming-events)]
         ^{:key (:event/id event)}
         [event-view (when (= 0 index) "Upcoming Sessions") event])
       (for [[index event] (map-indexed vector past-events)]
         ^{:key (:event/id event)}
         [event-view (when (= 0 index) "Past Sessions") event])]])])

(defn avoided-users-view []
  [ui/row {:title "Avoided Partners"}
   [:p {:tw "italic"} "Click on a " [fa/fa-ban-solid {:tw "w-4 h-4 inline-block"}] " in the session list below to prevent future matches with certain people."]
   (let [;; currently, getting list of users from past events
         ;; assuming that only avoid someone that have had an event with
         id->name (->> @(mod/subscribe [:events])
                       (map (fn [event]
                              [(:user/id (:event/other-guest event))
                               (:user/name (:event/other-guest event))]))
                       (into {}))
         names-avoided (map id->name (:user/user-pair-deny-list @(mod/subscribe [:user])))]
     [:div {:tw "p-2"}
      (if (seq names-avoided)
        names-avoided
        "(None)")])])

(defn p2p-page-view []
  [:div.page.p2p
   [ui/row
    {}
    [:div {:tw "text-sm space-y-2"}
     [:p "Get scheduled weekly for casual 1:1 study sessions with another student or mentor."]
     [:p "How it works: on Friday evening each week, you'll get an email asking if you're available for the next week. If you opt-in, on Sunday night you'll receive an email with your session schedule."]
     [:p "You can configure your availability, max # of sessions, and other settings below."]]]
   (when @(mod/subscribe [:user-profile-value :user/subscribed?])
     [:<>
      [opt-in-view]
      [warning-view]
      [common.profile/time-zone-view]
      [availability-view]
      [max-limit-preferences-view]
      [ui/row {}
       [:p {:tw "italic"} "The following fields from your profile affect who you are matched with:"]]
      [common.profile/role-view]
      [pair-with-view]
      [common.profile/language-views]
      [common.profile/topics-view]
      [avoided-users-view]
      [events-view]])
   [subscription-toggle-view]])

(mod/register-page!
  {:page/id :page.id/p2p
   :page/path "/p2p"
   :page/nav-label "Pairing"
   :page/view #'p2p-page-view
   :page/styles styles/styles
   :page/on-enter! (fn []
                     (mod/dispatch [:p2p/fetch-topics!])
                     (mod/dispatch [:p2p/fetch-events!]))})

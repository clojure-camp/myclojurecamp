(ns mycc.pairwithme.ui
  (:require
    [reagent.core :as r]
    [modulo.api :as mod]
    [mycc.common.availability :as availability]
    [mycc.common.profile :as common.profile]
    [mycc.common.ui :as ui]))

(def styles
  [:div.page.pairwithme
   availability/styles])

(defn format-slot
  [slot]
  (.format (js/Intl.DateTimeFormat.
             "en-US"
             #js {:weekday "short"
                  :month "short"
                  :day "numeric"
                  :hour "numeric"
                  :minute "2-digit"
                  :timeZone (or @(mod/subscribe [:user-profile-value :user/time-zone])
                                "UTC")})
           slot))

(defn has-availability? []
  (->> @(mod/subscribe [:user-profile-value :user/availability])
       vals
       (some some?)
       boolean))

(defn banner-message
  [{:keys [type message error accepted-slot]}]
  (case type
    (:success :pending)
    message

    :accept
    (case error
      nil
      (str "Accepted! You are pairing at " (format-slot accepted-slot) ". You should receive an email with a calendar invite.")

      :already-accepted
      (str "This session was already accepted for " (format-slot accepted-slot) ".")

      :replaced
      "This proposal was replaced by a newer one. See your sessions below."

      :wrong-user
      "Only the person who got this proposal can accept it. You seem to be logged in with the wrong account."

      :past
      [:<> "This time has passed. Create a new proposal."]

      "This proposal was not found.")))

(defn banner-view []
  (when-let [banner @(mod/subscribe [:pairwithme/banner])]
    [ui/row {}
     [:div {:tw ["p-4 rounded border flex justify-between gap-4"
                 (if (:error banner)
                   "bg-red-100 text-red-900 border-red-200"
                   "bg-green-100 text-green-900 border-green-200")]}
      [:div (banner-message banner)]
      [ui/secondary-button
       {:on-click (fn [_]
                    (mod/dispatch [:pairwithme/clear-banner!]))}
       "Close"]]]))

(defn explainer-view []
  [ui/row {}
   [:div {:tw "text-sm space-y-2"}
    [:p "Pair with others on Clojure. Add yourself to the list with projects you're interested in pairing on."]]])

(defn settings-view []
  [:<>
   [ui/row
    {:title "Show my profile?"
     :subtitle "Turn this off to hide yourself temporarily (ex. when busy, on vacation, etc)."}
    [ui/radio-list
     {:choices [[true "Yes"]
                [false "No"]]
      :value (boolean @(mod/subscribe [:user-profile-value :user/pairwithme-show-profile?]))
      :on-change (fn [value]
                   (mod/dispatch [:set-user-value! :user/pairwithme-show-profile? (boolean value)]))}]
    (when (and @(mod/subscribe [:user-profile-value :user/pairwithme-show-profile?])
               (not (has-availability?)))
      [ui/warning {} "Others can't see your profile until you add some availability below."])]
   [ui/row
    {:title "I'd like to pair on..."
     :subtitle [:span "A bullet list of what you'd like to pair on." [:br] "Ex. Advent of Code, learning reagent, contributing to Jank, etc."]}
    [ui/textarea
     {:default-value @(mod/subscribe [:user-profile-value :user/pairwithme-topics])
      :on-change (fn [e]
                   (mod/dispatch
                     [:debounced-set-user-value! :user/pairwithme-topics (.. e -target -value)]))}]]
   [common.profile/time-zone-view]
   [availability/availability-view {:show-events? false}]])

(defn session-view
  [{:keys [other-user status] :as session}]
  [:div {:tw "flex justify-between items-center gap-4 p-3 rounded border"}
   (case status
     :accepted
     [:div "Pairing with " [:strong (:user/name other-user)] ": "
      (format-slot (:pairwithme-session/accepted-slot session))]

     :respond
     [:div [:strong (:user/name other-user)] " proposed times. Check your email to accept one."]

     :waiting
     [:div
      [:div "Waiting for " [:strong (:user/name other-user)] " to respond. You proposed:"]
      [:ul {:tw "list-disc pl-5"}
       (for [slot (:proposal/slots (last (:pairwithme-session/proposals session)))]
         ^{:key (.getTime slot)}
         [:li (format-slot slot)])]])
   (when (= :respond status)
     [ui/secondary-button
      {:on-click (fn [_]
                   (mod/dispatch [:pairwithme/open-modal! {:other-user other-user
                                                           :session-id (:pairwithme-session/id session)}]))}
      "Propose other times"])])

(defn sessions-view []
  (when-let [sessions (seq @(mod/subscribe [:pairwithme/sessions]))]
    [ui/row
     {:title "Upcoming Pairing Sessions"}
     [:div {:tw "space-y-2"}
      (for [session sessions]
        ^{:key (:pairwithme-session/id session)}
        [session-view session])]]))

(defn profile-view
  [profile]
  [:div {:tw "flex justify-between items-start gap-4 py-2 border-b-1 last:border-b-0"}
   [:div
    [:div
     [:a {:href (str "/community#" (:user/id profile))
          :tw "font-bold"}
      (:user/name profile)]]
    (when-let [topics (:user/pairwithme-topics profile)]
      [:<>
       [:div {:tw "text-sm"} "Would like to pair on:"]
       [:div {:tw "text-sm whitespace-pre"}
        topics]])]
   [ui/button
    {:on-click (fn [_]
                 (mod/dispatch [:pairwithme/open-modal! {:other-user profile}]))}
    "Let's Pair"]])

(defn profiles-view []
  (r/with-let [only-overlapping? (r/atom (has-availability?))]
    [ui/row
     {}
     [:label {:tw "flex gap-1 items-center text-sm cursor-pointer"}
      [:input {:type "checkbox"
               :checked @only-overlapping?
               :on-change (fn [_]
                            (swap! only-overlapping? not))}]
      "Only show people with overlapping availability"]
     (let [profiles (cond->> @(mod/subscribe [:pairwithme/profiles])
                      @only-overlapping?
                      (filter (fn [profile]
                                (pos? (:overlap-count profile)))))]
       (if (seq profiles)
         [:div
          (for [profile profiles]
            ^{:key (:user/id profile)}
            [profile-view profile])]
         [:p {:tw "italic"} "(No one yet)"]))]))

(defn cell-view
  [{:keys [slot viewer-value other-value]} selected-slots]
  (let [slot-ms (when slot (.getTime slot))
        selected? (contains? selected-slots slot-ms)]
    [:td {:tw "p-0.5"}
     (if other-value
       [:button
        {:tw ["block w-full h-8 rounded text-xs cursor-pointer border"
              (cond
                selected?
                "bg-clojure-camp-blue text-white border-clojure-camp-blue"

                viewer-value
                "bg-blue-300 border-blue-400 hover:bg-blue-400"

                :else
                "bg-blue-100 border-blue-200 hover:bg-blue-200")]
         :on-click (fn [_]
                     (mod/dispatch [:pairwithme/toggle-slot! slot-ms]))}
        (cond
          selected?
          "✓"

          (= :preferred other-value)
          [:span {:tw "text-clojure-camp-blue"} "★"])]
       [:div {:tw "w-full h-8 bg-gray-100 rounded"}])]))

(defn schedule-grid-view
  [schedule selected-slots]
  (let [dates (->> schedule
                   (map (fn [cell]
                          (select-keys cell [:local-date
                                             :local-day-of-week-label
                                             :local-date-label])))
                   distinct)
        available-hours (->> schedule
                             (filter :other-value)
                             (map :local-hour))
        hours (when (seq available-hours)
                (range (apply min available-hours)
                       (inc (apply max available-hours))))
        cells (->> schedule
                   (map (fn [cell]
                          [[(:local-date cell) (:local-hour cell)] cell]))
                   (into {}))]
    (if (empty? hours)
      [:p {:tw "italic"} "This person has no availability in the next 7 days."]
      [:div {:tw "py-4"}
       [:table {:tw "w-full text-sm"}
        [:thead
         [:tr
          [:th]
          (for [{:keys [local-date local-day-of-week-label local-date-label]} dates]
            ^{:key local-date}
            [:th {:tw "font-normal px-1 pb-1"}
             [:div local-day-of-week-label]
             [:div {:tw "text-#aaa"} local-date-label]])]]
        [:tbody
         ;; filler row, so that the header doesn't cover the offset hour label
         [:tr
          [:td {:tw "h-2"}]]
         (for [hour hours]
           ^{:key hour}
           [:tr
            [:td {:tw "text-right pr-4 align-top font-light"
                  :style {:transform "translateY(-0.5rem)"}}
             hour ":00"]
            (for [{:keys [local-date]} dates]
              ^{:key local-date}
              [cell-view (get cells [local-date hour]) selected-slots])])]]])))

(defn modal-view []
  (when-let [{:keys [other-user schedule selected-slots sending? error]}
             @(mod/subscribe [:pairwithme/modal])]
    [:div {:tw "fixed inset-0 z-1000 flex items-center justify-center p-4"
           :style {:background "rgba(0,0,0,0.5)"}
           :on-click (fn [e]
                       (when (= (.-target e) (.-currentTarget e))
                         (mod/dispatch [:pairwithme/close-modal!])))}
     [:div {:tw "bg-white rounded p-4 max-w-3xl w-full max-h-full overflow-y-auto space-y-3"}
      [:div {:tw "flex justify-between items-baseline gap-4"}
       [:h1 {:tw "font-bold"}
        "Pick 1 - 3 times to suggest to " (:user/name other-user)]
       [:div {:tw "text-xs text-gray-600"}
        (or @(mod/subscribe [:user-profile-value :user/time-zone])
            "UTC")]]
      (if schedule
        [schedule-grid-view schedule selected-slots]
        [:p {:tw "italic"} "Loading..."])
      (when error
        [ui/warning {} error])
      [:div {:tw "flex justify-between items-center gap-4"}
       [:div {:tw "flex gap-3 items-center text-xs"}
        [:span {:tw "flex gap-1 items-center"}
         [:span {:tw "inline-block w-4 h-4 bg-blue-300 border border-blue-400 rounded"}]
         "Both Available"]
        [:span {:tw "flex gap-1 items-center"}
         [:span {:tw "inline-block w-4 h-4 bg-blue-100 border border-blue-200 rounded"}]
         (str (:user/name other-user) " Available")]
        [:span {:tw "flex gap-1 items-center"}
         [:span {:tw "text-clojure-camp-blue"} "★"]
         (str (:user/name other-user) " Preferred")]]
       [:div {:tw "flex gap-2"}
        [ui/secondary-button
         {:on-click (fn [_]
                      (mod/dispatch [:pairwithme/close-modal!]))}
         "Cancel"]
        [ui/button
         {:disabled (or sending?
                        (empty? selected-slots))
          :on-click (fn [_]
                      (mod/dispatch [:pairwithme/propose!]))}
         (str "Send proposal (" (count selected-slots) ")")]]]]]))

(defn pairwithme-page-view [_]
  [:div.page.pairwithme
   [banner-view]
   [explainer-view]
   [sessions-view]
   [ui/section-header-view "Find Someone To Pair With"]
   [ui/row {}
    [:div {:tw "text-sm space-y-2"}
     [:p "Check out people's interests below and select \"Let's Pair\" to propose a time."]
     [:p "Proposals are sent by email to the other person. When they accept, you both get a confirmation email with a calendar invite. Reply to that email to chat with your partner."]]]
   [profiles-view]
   [ui/section-header-view "Add Yourself to the List"]
   [settings-view]
   [modal-view]])

(mod/register-page!
  {:page/id :page.id/pairwithme
   :page/path "/pairwithme"
   :page/nav-label "Pairing"
   :page/parameters [:map [:session {:optional true} :uuid]]
   :page/view #'pairwithme-page-view
   :page/styles styles
   :page/on-enter! (fn [[_ {:keys [session]}]]
                     (mod/dispatch [:pairwithme/fetch-profiles!])
                     (mod/dispatch [:pairwithme/fetch-sessions! {:open-counter-for session}]))})

(mod/register-page!
  {:page/id :page.id/pairwithme-accept
   :page/path "/pairwithme/accept/:session-id/:proposal-id/:slot-index"
   :page/parameters [:map
                     [:session-id :uuid]
                     [:proposal-id :uuid]
                     [:slot-index :int]]
   :page/view #'pairwithme-page-view
   :page/styles styles
   :page/on-enter! (fn [[_ params]]
                     (mod/dispatch [:pairwithme/fetch-profiles!])
                     (mod/dispatch [:pairwithme/accept! params]))})

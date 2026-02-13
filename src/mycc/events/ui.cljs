(ns mycc.events.ui
  (:require
   [modulo.api :as mod]
   [mycc.common.profile :as common.profile]
   [mycc.common.ui :as ui]
   [mycc.common.availability :as availability]))

(def styles
  [:div.page.events
   availability/styles])

(defn events-page-view []
  [:div.page.events
   [ui/row
    {:title "Which events are you interested in?"
     :subtitle [:span {:tw "text-sm"}
                "This lets us know which events to notify you about and to help schedule events."]}
    [:div
     [ui/checkbox-list
      {:value (or @(mod/subscribe [:user-profile-value :user/events-interested-in]) #{})
       :choices [[:event/mob-sessions "Mob Sessions"]
                 [:event/pairing "1-on-1 Pairing"]]
       :on-change (fn [value]
                    (mod/dispatch [:set-user-value!
                                  :user/events-interested-in value]))}]]]
   [ui/row
    {}
    [:div {:tw "text-sm space-y-2"}
     [:p "Use the grid below to indicate your general availability for Clojure Camp events."]
     [:p "We will use this information when scheduling events (such as mob sessions)."]
     [:p "You can indicate 'available' and 'preferred' by clicking repeatedly on a cell."]
     [:p "(If you want to Pair 1:1, you still need to opt-in weekly on the 'Pairing' tab.)"]]]

   [common.profile/time-zone-view]
   [availability/availability-view {:show-events? false}]])

(mod/register-page!
 {:page/id :page.id/events
  :page/path "/events"
  :page/nav-label "events"
  :page/view #'events-page-view
  :page/styles styles})

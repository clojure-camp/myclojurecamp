(ns mycc.event-crosscheck
  (:require
   [bloom.commons.html :as html]
   [mycc.common.discord :as discord]
   [mycc.p2p.meetups :as meetups]
   [zprint.core :as zprint])
  (:import
   (java.time ZonedDateTime ZoneId)))

(defn discord-event->meetup [event]
  (let [day (get [:monday :tuesday :wednesday
                  :thursday :friday :saturday :sunday]
                 (get-in event [:recurrence_rule :by_n_weekday 0 :day]))
        start (-> (get-in event [:recurrence_rule :start])
                  ZonedDateTime/parse
                  (.withZoneSameInstant (ZoneId/of "America/Toronto")))]
    {:meetup/title (:name event)
     :meetup/day day
     :meetup/week (get-in event [:recurrence_rule :by_n_weekday 0 :n])
     :meetup/timezone "America/Toronto"
     :meetup/start-hour (.getHour start)
     :meetup/duration-hours 2}))

(defn match-by [f item coll]
  (some (fn [i] (when (= (f item) (f i)) i)) coll))

(defn format [x]
  [:pre
   (zprint/zprint-str x 80 {:style [:community :hiccup]
                            :map {:comma? false
                                  :sort? true
                                  :lift-ns? false
                                  :force-nl? true}})])

; Other event sources: Meetup.com, Google Calendar

(defn check []
  (let [discord-events (->> (discord/list-guilds)
                            first
                            :id
                            discord/list-guild-events
                            (filter :recurrence_rule)
                            (map discord-event->meetup)
                            (sort-by (juxt :meetup/week :meetup/day)))
        meetup-events (sort-by (juxt :meetup/week :meetup/day) (meetups/all))
        f (fn [event] (dissoc event :meetup/title))]
    [:table
     [:tr [:th "Meet-Up"] [:th "Discord Events"]]
     (for [event meetup-events]
       [:tr
        [:td (format event)]
        [:td (format (match-by f event discord-events))]])
     [:tr
      [:td {:col-span 2
            :style {:border-bottom "1px solid black"}} "Not Matching"]]
     (for [event (remove (fn [ev] (match-by f ev meetup-events)) discord-events)]
       [:tr
        [:td]
        [:td (format event)]])]))

(comment
  (spit "table.html" (html/render (check)))
  (discord-event->meetup {:name "Mob Programming w/ Raf (A)"
                          :id "1352755326302163066"
                          :scheduled_start_time "2025-05-01T22:00:00+00:00"
                          :recurrence_rule
                          {:by_year_day nil
                           :by_month_day nil
                           :by_month nil
                           :by_weekday nil
                           :frequency 1
                           :start "2025-04-03T22:00:00+00:00"
                           :count nil
                           :by_n_weekday [{:n 1, :day 3}]
                           :interval 1
                           :end nil}}))

#_{:meetup/title "Mob w/ Raf"
   :meetup/day :wednesday
   :meetup/week 3
   :meetup/timezone "America/Toronto"
   :meetup/start-hour 18
   :meetup/duration-hours 2}

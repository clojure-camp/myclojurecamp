(ns mycc.event-crosscheck
  (:require
   [bloom.commons.html :as html]
   [mycc.common.discord :as discord]
   [mycc.common.gcal :as gcal]
   [mycc.common.meetupdotcom :as meetupdotcom]
   [mycc.p2p.meetups :as meetups]
   [zprint.core :as zprint]
   [clojure.string :as str])
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

(defn gcal-event->meetup [{:keys [recurrence start] :as event}]
  (let [{:keys [day week]} (let [[_ week day] (re-matches #".*BYDAY=(\d)(\w+)"
                                                          (first recurrence))]
                             {:week (parse-long week)
                              :day ({"SU" :sunday
                                     "MO" :monday
                                     "TU" :tuesday
                                     "WE" :wednesday
                                     "TH" :thursday
                                     "FR" :friday
                                     "SA" :saturday} day)})
        time-zone (:timeZone start)
        start-time (-> start
                       :dateTime
                       ZonedDateTime/parse
                       (.withZoneSameInstant (ZoneId/of time-zone)))]
    {:meetup/title (:summary event)
     :meetup/day day
     :meetup/week week
     :meetup/timezone time-zone
     :meetup/start-hour (.getHour start-time)
     ;; could figure out, but meh.
     :meetup/duration-hours 2}))

(defn meetupdotcom-event->meetup [{:keys [title series dateTime] :as event}]
  (let [day (keyword
             (str/lower-case
              (get-in series [:monthlyRecurrence :monthlyDayOfWeek])))
        week (get-in series [:monthlyRecurrence :monthlyWeekOfMonth])
        time-zone "America/Toronto"
        start-time (.withZoneSameInstant (ZonedDateTime/parse dateTime)
                                         (ZoneId/of time-zone))]
    {:meetup/title title
     :meetup/day day
     :meetup/week week
     :meetup/timezone time-zone
     :meetup/start-hour (.getHour start-time)
     ;; could figure out, but meh.
     :meetup/duration-hours 2}))

(defn match-by [f item coll]
  (some (fn [i] (when (= (f item) (f i)) i)) coll))

(defn format-event [x]
  [:pre
   (zprint/zprint-str x 80 {:style [:community :hiccup]
                            :map {:comma? false
                                  :sort? true
                                  :lift-ns? false
                                  :force-nl? true}})])

; Other event sources: Meetup.com

(defn check []
  (let [discord-events (->> (discord/list-guilds)
                            first
                            :id
                            discord/list-guild-events
                            (filter :recurrence_rule)
                            (map discord-event->meetup)
                            (sort-by (juxt :meetup/week :meetup/day)))
        gcal-events (->> (gcal/calendar-events
                          {:calendar-id "admin@clojure.camp"})
                         (map gcal-event->meetup)
                         (sort-by (juxt :meetup/week :meetup/day)))
        meetupdotcom-events (->> (meetupdotcom/meetup-events)
                                 :data
                                 :groupByUrlname
                                 :events
                                 :edges
                                 (map :node)
                                 (map meetupdotcom-event->meetup))
        meetup-events (sort-by (juxt :meetup/week :meetup/day) (meetups/all))
        f (fn [event] (dissoc event :meetup/title))]
    [:table
     [:tr [:th "From Config"] [:th "Discord Events"]
      [:th "G Calendar"] [:th "Meetup.com Events"]]
     (for [event meetup-events]
       [:tr
        [:td (format-event event)]
        [:td (format-event (match-by f event discord-events))]
        [:td (format-event (match-by f event gcal-events))]
        [:td (format-event (match-by f event meetupdotcom-events))]])
     [:tr
      [:td {:col-span 2
            :style {:border-bottom "1px solid black"}} "Not Matching"]]
     (for [event (remove (fn [ev] (match-by f ev meetup-events)) discord-events)]
       [:tr
        [:td]
        [:td (format-event event)]
        [:td]
        [:td]])
     (for [event (remove (fn [ev] (match-by f ev meetup-events)) gcal-events)]
       [:tr
        [:td]
        [:td]
        [:td (format-event event)]
        [:td]])
     (for [event (remove (fn [ev] (match-by f ev meetup-events)) meetupdotcom-events)]
       [:tr
        [:td]
        [:td]
        [:td]
        [:td (format-event event)]])]))

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

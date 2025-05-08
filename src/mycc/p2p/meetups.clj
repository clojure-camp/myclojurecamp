(ns mycc.p2p.meetups
  (:require
   [hyperfiddle.rcf :as rcf]
   [mycc.common.date :as date])
  (:import
   (java.util Date)
   (java.time LocalTime ZoneId ZonedDateTime)
   (java.time.temporal TemporalAdjusters)))

(defn next-meetup-insts
  [now-inst {:meetup/keys [_title day week timezone start-hour duration-hours]}]
  (let [now-zdt (.adjustInto (LocalTime/of start-hour 0)
                             (ZonedDateTime/ofInstant (.toInstant now-inst)
                                                      (ZoneId/of timezone)))
        start (.with now-zdt
                     (TemporalAdjusters/dayOfWeekInMonth week (date/->java-day-of-week day)))
        ;; above might generate a date in the past
        start (if (.before (Date/from (.toInstant start)) now-inst)
                (.with (.plusMonths now-zdt 1)
                       (TemporalAdjusters/dayOfWeekInMonth week (date/->java-day-of-week day)))
                start)]
    (->> (range duration-hours)
         (map (fn [plus-hours]
                (.plusHours start plus-hours)))
         (map #(Date/from (.toInstant %))))))

(rcf/tests
 (next-meetup-insts
  #inst "2025-05-01"
  {:meetup/day :wednesday
   :meetup/week 1
   :meetup/timezone "America/Toronto"
   :meetup/start-hour 18
   :meetup/duration-hours 2})
 :=
 [#inst "2025-05-07T22:00:00.000-00:00"
  #inst "2025-05-07T23:00:00.000-00:00"]

 (next-meetup-insts
  #inst "2025-05-12"
  {:meetup/day :wednesday
   :meetup/week 1
   :meetup/timezone "America/Toronto"
   :meetup/start-hour 18
   :meetup/duration-hours 2})
 :=
 [#inst "2025-06-04T22:00:00.000-00:00"
  #inst "2025-06-04T23:00:00.000-00:00"])

(defn local-date->inst
  [local-date]
  (-> local-date
      (.atStartOfDay (ZoneId/systemDefault))
      (.toInstant)
      (java.util.Date/from)))

#_(next-meetup-insts #inst "2025-04-17T10:00:00.000-00:00"
                     {:meetup/title "Mob w/ Raf"
                      :meetup/day :wednesday
                      :meetup/week 4
                      :meetup/timezone "America/Toronto"
                      :meetup/start-hour 18
                      :meetup/duration-hours 2})

(defn all-meetup-insts
  [start-of-week meetups]
  (->> meetups
       (mapcat (partial next-meetup-insts (local-date->inst start-of-week)))
       set))

#_(all-meetup-insts (java.time.LocalDate/now) [{:meetup/title "Mob w/ Raf"
                                                :meetup/day :wednesday
                                                :meetup/week 3
                                                :meetup/timezone "America/Toronto"
                                                :meetup/start-hour 18
                                                :meetup/duration-hours 2}
                                               {:meetup/title "Mob w/ Raf"
                                                :meetup/day :wednesday
                                                :meetup/week -1
                                                :meetup/timezone "America/Toronto"
                                                :meetup/start-hour 18
                                                :meetup/duration-hours 2}])

#_(all-meetup-insts (java.time.LocalDate/now) (modulo.api/config :meetups))

#_(let [now (local-date->inst (java.time.LocalDate/now))]
    (->> (modulo.api/config :meetups)
         (map (juxt :meetup/title (partial next-meetup-insts now)))))

(defn next-week-meetups-in-local-time
  [time-zone meetups]
  (let [start-of-week (date/next-monday)
        start-of-next-week (.with
                            start-of-week
                            (TemporalAdjusters/next (date/->java-day-of-week :monday)))]
    (->> (all-meetup-insts start-of-week meetups)
         (map (fn [inst]
                (ZonedDateTime/ofInstant (.toInstant inst)
                                         (ZoneId/of time-zone))))
         ;; filter out events more than a week in the future
         (filter (fn [zdt]
                   (.isBefore zdt (.atZone (.atStartOfDay start-of-next-week)
                                           (ZoneId/of time-zone)))))
         (map (fn [zdt]
                [(-> (.getDayOfWeek zdt)
                     (date/java-day-of-week->))
                 (.getHour zdt)]))
         set)))

#_(next-week-meetups-in-local-time
   "America/Vancouver"
   [{:meetup/title "Mob w/ Raf"
     :meetup/day :wednesday
     :meetup/week 1
     :meetup/timezone "America/Toronto"
     :meetup/start-hour 18
     :meetup/duration-hours 2}
    {:meetup/title "Mob w/ Raf"
     :meetup/day :wednesday
     :meetup/week 2
     :meetup/timezone "America/Toronto"
     :meetup/start-hour 18
     :meetup/duration-hours 2}])

;; #{[:wednesday 15] [:wednesday 16]}

(ns mycc.p2p.meetups
  (:require
   [mycc.common.date :as date])
  (:import
   (java.time LocalTime ZoneId ZonedDateTime)
   (java.time.temporal TemporalAdjusters)))

(defn next-meetup-insts
  [now-inst {:meetup/keys [_title day week timezone start-hour duration-hours]}]
  (let [start (.with
               (.adjustInto (LocalTime/of start-hour 0)
                            (ZonedDateTime/ofInstant (.toInstant now-inst)
                                                     (ZoneId/of timezone)))
               (TemporalAdjusters/dayOfWeekInMonth week (date/->java-day-of-week day)))]
    (->> (range duration-hours)
         (map (fn [plus-hours]
                (.plusHours start plus-hours)))
         (map #(java.util.Date/from (.toInstant %))))))

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

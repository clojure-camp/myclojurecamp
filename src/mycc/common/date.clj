(ns mycc.common.date
  (:refer-clojure :exclude [format])
  (:import
    (java.time DayOfWeek ZonedDateTime ZoneId LocalTime LocalDate)
    (java.time.temporal TemporalAdjusters)
    (java.time.format DateTimeFormatter)))

(def ->java-day-of-week
  {:monday DayOfWeek/MONDAY
   :tuesday DayOfWeek/TUESDAY
   :wednesday DayOfWeek/WEDNESDAY
   :thursday DayOfWeek/THURSDAY
   :friday DayOfWeek/FRIDAY
   :saturday DayOfWeek/SATURDAY
   :sunday DayOfWeek/SUNDAY})

(defn adjust-day-of-week
  "Given a local-date, adjusts into the following day of week
     ex. 2021-01-04 + :thursday -> 2021-01-06"
  [local-date day-of-week]
  (.with
    local-date
    (TemporalAdjusters/nextOrSame (->java-day-of-week day-of-week))))

#_(adjust-day-of-week (LocalDate/now) :friday)

(defn upcoming-monday []
  (adjust-day-of-week
    (LocalDate/now) :monday))

(defn convert-time
  "Converts from [:thursday 19] + 'America/Vancouver' (user's preferences)
  + 2021-12-01  ('Monday' for which we run the matching)
  to ZonedDateTime 2021-12-01 19:00:00 UTC"
  [[day-of-week hour-of-day] user-time-zone-string reference-local-date]
  (.withZoneSameInstant (ZonedDateTime/of (.with
                                             (adjust-day-of-week reference-local-date :monday)
                                             (TemporalAdjusters/nextOrSame (->java-day-of-week day-of-week)))
                                           (LocalTime/of hour-of-day 0)
                                           (ZoneId/of user-time-zone-string))
                        (ZoneId/of "UTC")))

#_(convert-time [:friday 19] "America/Vancouver" (LocalDate/now))
#_(LocalTime/of 19 0)
#_(LocalDate/now)

(defn format [at pattern]
  (.format (ZonedDateTime/ofInstant (.toInstant at)
                                    (ZoneId/of "UTC"))
           (DateTimeFormatter/ofPattern pattern)))

(ns mycc.pairwithme.announcement-job
  (:require
    [chime.core :as chime]
    [modulo.api :as mod]
    [mycc.common.db :as db]
    [mycc.common.discord :as discord]
    [mycc.pairwithme.slots :as slots])
  (:import
    (java.time DayOfWeek LocalTime Period ZoneId ZonedDateTime)))

(defn announcement-message
  [listed-count]
  (str listed-count " people are looking to pair this week: "
       (mod/config :app-domain) "/pairwithme"))

(defn post-announcement! []
  (let [channel-id (mod/config [:discord :pairwithme-channel-id])
        listed-count (count (filter slots/listed? (db/get-users)))]
    (when (and channel-id
               (< 1 listed-count))
      (discord/post-message! channel-id (announcement-message listed-count)))))

#_(post-announcement!)

(defn schedule-announcement-job! []
  (chime/chime-at
    (->> (chime/periodic-seq
           (.adjustInto (LocalTime/of 12 0)
                        (ZonedDateTime/now (ZoneId/of "America/Toronto")))
           (Period/ofDays 1))
         (filter (fn [instant]
                   (= DayOfWeek/SATURDAY (.getDayOfWeek instant)))))
    (fn [_]
      (post-announcement!))))

(mod/register-job! ::job schedule-announcement-job!)

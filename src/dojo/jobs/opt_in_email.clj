(ns dojo.jobs.opt-in-email
  (:require
    [chime.core :as chime]
    [dojo.config :refer [config]]
    [dojo.email :as email])
  (:import
    (java.time Period DayOfWeek ZonedDateTime ZoneId LocalTime)
    (java.time.format DateTimeFormatter)
    (java.time.temporal TemporalAdjusters)))

(defn datetime->next-monday-string [zoned-datetime]
  (.format (.with zoned-datetime
            (TemporalAdjusters/next DayOfWeek/MONDAY))
           (DateTimeFormatter/ofPattern "MMM dd")))

(defn friday-email-template [email]
  {:to email
   :subject "ClojoDojo - Pair Next Week?"
   :body [:div
          [:p
           "If you want to pair next week, put an X in the \"Pair Week of "
           (datetime->next-monday-string
             (ZonedDateTime/now (ZoneId/of "America/Toronto"))) "\" column in "
           [:a {:href "https://docs.google.com/spreadsheets/d/1XYJjfHQzWu1_WiOV33Fu4RvJn3D1VZ3AyGbE__w2z2g/edit#gid=0"} "the spreadsheet"] " and update your availability (the calendar now includes evenings)."]
          [:p "If you're no longer interested, remove yourself from the spreadsheet."]
          [:p "I'll send the schedule Sunday night."]
          [:p "R"]]})

(defn send-friday-emails! []
  (doseq [email (config :emails)]
    (email/send! (friday-email-template email))))

(defn schedule-email-job! []
  (chime/chime-at
    (->> (chime/periodic-seq
           (.adjustInto (LocalTime/of 21 0)
                        (ZonedDateTime/now (ZoneId/of "America/Toronto")))
           (Period/ofDays 1))
         (filter (fn [instant]
                   (= DayOfWeek/FRIDAY (.getDayOfWeek instant)))))
    (fn [_]
      (send-friday-emails!))))

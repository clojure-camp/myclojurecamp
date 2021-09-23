(ns dojo.jobs.opt-in-email
  (:require
    [chime.core :as chime]
    [dojo.config :refer [config]]
    [dojo.email :as email]
    [dojo.db :as db])
  (:import
    (java.time Period DayOfWeek ZonedDateTime ZoneId LocalTime)
    (java.time.format DateTimeFormatter)
    (java.time.temporal TemporalAdjusters)))

(defn datetime->next-monday-string [zoned-datetime]
  (.format (.with zoned-datetime
            (TemporalAdjusters/next DayOfWeek/MONDAY))
           (DateTimeFormatter/ofPattern "MMM dd")))

(defn friday-email-template [user]
  {:to (:user/email user)
   :subject "ClojoDojo - Pair Next Week?"
   :body [:div
          [:p "Hey " (:user/name user) ","]
          [:p "If you want to pair next week, "
              [:a {:href (@config :app-domain)} "opt-in and update your availability schedule"] "."]
          [:p "The schedule will be sent Sunday night."]
          [:p "- clojodojo bot"]]})

#_(friday-email-template "alice@example.com")

(defn send-friday-emails! []
  (doseq [user (remove :user/pair-next-week? (db/get-users))]
    (email/send! (friday-email-template user))))

#_(send-friday-emails!)

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

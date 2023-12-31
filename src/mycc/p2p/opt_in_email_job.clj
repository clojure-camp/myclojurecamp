(ns mycc.p2p.opt-in-email-job
  (:require
    [bloom.omni.auth.token :as token]
    [chime.core :as chime]
    [modulo.api :as mod]
    [mycc.common.email :as email]
    [mycc.common.db :as db])
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
   :subject "Clojure Camp - Pair Next Week?"
   :body [:div
          [:p "Hey " (:user/name user) ","]
          [:p "If you want to pair next week, "
              [:a {:href (str (mod/config :app-domain)
                              "/p2p?"
                              (token/login-query-string (:user/id user) (mod/config :auth-token-secret)))} "opt-in and update your availability schedule"] "."]
          [:p "The schedule will be sent Sunday night."]
          [:p "- Clojure Camp scheduler bot"]]})

#_(email/send! (friday-email-template (first (db/get-users))))

(defn send-friday-emails! []
 (->> (db/get-users)
      (filter :user/subscribed?)
      (remove :user/pair-next-week?)
      (map (fn [user]
            (email/send! (friday-email-template user))))
      dorun))

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

(mod/register-job! ::job schedule-email-job!)

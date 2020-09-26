(ns dojo.core
  (:require
    [chime.core :as chime]
    [postal.core :as postal]
    [hiccup.core :as hiccup]
    [bloom.commons.config :as config])
  (:import
    (java.time Period DayOfWeek ZonedDateTime ZoneId LocalTime)
    (java.time.format DateTimeFormatter)))

(def config
  (config/read
   "config.edn"
   [:map
    [:emails [:vector string?]]
    [:smtp-credentials
     [:map
      [:port integer?]
      [:host string?]
      [:ssl boolean?]
      [:from string?]
      [:user string?]
      [:pass string?]]]]))

(defn send! [{:keys [to subject body]}]
  (postal/send-message
    (:smtp-credentials config)
    {:from (:from (:smtp-credentials config))
     :to to
     :subject subject
     :body [{:type "text/html; charset=utf-8"
             :content (hiccup/html body)}]}))

(defn datetime->next-monday-string [zoned-datetime]
  ;; assume we send on Fridays
  ;; TODO instead, adjust date into 'next Monday'
  (.format (.plusDays zoned-datetime 3)
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
    (send! (friday-email-template email))))

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

(defn -main []
  (schedule-email-job!))

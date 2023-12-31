(ns mycc.p2p.match-email-job
  (:require
    [clojure.string :as string]
    [clojure.set :as set]
    [bloom.commons.uuid :as uuid]
    [chime.core :as chime]
    [pairing-scheduler.core :as ps]
    [mycc.p2p.util :as util]
    [mycc.common.email :as email]
    [mycc.common.db :as db]
    [mycc.p2p.db :as p2p.db]
    [mycc.api :as api])
  (:import
    (java.time Period DayOfWeek ZonedDateTime ZoneId LocalTime LocalDate)
    (java.time.format DateTimeFormatter)
    (java.time.temporal TemporalAdjusters ChronoUnit)))

(def ->java-day-of-week
  {:monday DayOfWeek/MONDAY
   :tuesday DayOfWeek/TUESDAY
   :wednesday DayOfWeek/WEDNESDAY
   :thursday DayOfWeek/THURSDAY
   :friday DayOfWeek/FRIDAY
   :saturday DayOfWeek/SATURDAY
   :sunday DayOfWeek/SUNDAY})

(defn mapify
  [kf vf coll]
  (zipmap (map kf coll)
          (map vf coll)))

(defn adjust-day-of-week
  "Given a local-date, adjusts into the following day of week
     ex. 2021-01-04 + :thursday -> 2021-01-06"
  [local-date day-of-week]
  (.with
    local-date
    (TemporalAdjusters/nextOrSame (->java-day-of-week day-of-week))))

#_(adjust-day-of-week (LocalDate/now) :friday)

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

(defn ->inst [zoned-date-time]
  (java.util.Date/from (.toInstant zoned-date-time)))

(defn prep-input-for-schedule
  [users local-date-start-of-week]
  {:times-to-pair 1
   :max-events-per-day (mapify :user/id :user/max-pair-per-day users)
   :max-events-per-week (mapify :user/id :user/max-pair-per-week users)
   :topics (mapify :user/id :user/topic-ids users)
   :timezones (mapify :user/id :user/time-zone users)
   :roles (-> (mapify :user/id :user/role users)
              (update-vals (fn [role]
                             #{role})))
   :roles-to-pair-with (-> (mapify :user/id :user/role users)
                           (update-vals (fn [role]
                                          (case role
                                            :role/student
                                            #{:role/student :role/mentor}
                                            :role/mentor
                                            #{:role/student}))))
   :availabilities (mapify :user/id
                           ;; stored as {[:monday 10] :available
                           ;;            [:tuesday 10] :preferred
                           ;;            [:wednesday 10] nil)
                           ;; but need #{[:monday 10 :available]
                           ;;            [:tuesday 10 :preferred]}
                           ;; also, remove when value is nil
                           (fn [user]
                             (->> (:user/availability user)
                                  (filter (fn [[_ v]] v))
                                  (map (fn [[k v]]
                                         [(->inst (convert-time k (:user/time-zone user) local-date-start-of-week)) v]))
                                  set))
                           users)})

#_(prep-input-for-schedule (db/get-users) (LocalDate/now))

(defn generate-schedule
  "Returns a list of maps, with :event/guest-ids, :day-of-week and :Time-of-day,
    ex.
    [{:event/guest-ids #{123 456}
      :event/at #inst \"...\"} ...]"
  [users local-date-start-of-week]
  (if (empty? users)
   []
   (->> (prep-input-for-schedule users local-date-start-of-week)
        (ps/schedule)
        :schedule
        (map (fn [event]
              (-> event
                  (set/rename-keys {:at :event/at
                                    :guest-ids :event/guest-ids})
                  (assoc :event/id (uuid/random))))))))

#_(generate-schedule (db/get-users) (LocalDate/now))

(defn group-by-guests
  [schedule]
  (reduce (fn [memo event]
           (-> memo
               (update (first (:event/guest-ids event)) (fnil conj #{}) event)
               (update (last (:event/guest-ids event)) (fnil conj #{}) event)))
     {} schedule))

(defn ->date-string [at]
  (.format (ZonedDateTime/ofInstant (.toInstant at)
                                    (ZoneId/of "UTC"))
           (DateTimeFormatter/ofPattern "yyyy-MM-dd")))

(defn ->topics [event]
  (->> (:event/guest-ids event)
       (map db/get-user)
       (map :user/topic-ids)
       (apply set/intersection)
       (map p2p.db/get-topic)
       (map :topic/name)
       (string/join ", ")))

#_(->topics {:event/guest-ids #{(:user/id (first (db/get-users)))
                                (:user/id (last (db/get-users)))}
             :event/at #inst "2021-11-08T14:00:00.000-00:00"})

(defn unmatched-email-template
  [user-id]
  (let [user (db/get-user user-id)]
    {:to (:user/email user)
     :subject "Clojure Camp - Your Matches for this Week"
     :body [:div
            [:p "Hi " (:user/name user) ","]
            [:p "Unfortunately, we couldn't match you with anyone this week. :("]
            [:p "- Clojure Camp scheduler bot"]]}))

(defn event->ical
  [{:event/keys [guest-ids at id] :as event}]
  (let [guests (map db/get-user guest-ids)
        ;; iCAL expects datetimes in the form: "20211108T140000Z"
        format  (fn [t]
                  (string/replace t #"-|:" ""))
        start (.toInstant at)
        end (.plus start 1 ChronoUnit/HOURS)]
   (->> [["BEGIN" "VCALENDAR"]
         ["VERSION" "2.0"]
         ["PRODID" "CLOJURE.CAMP"]
         ["CALSCALE" "GREGORIAN"]
         ["METHOD" "PUBLISH"]
         ["BEGIN" "VEVENT"]
         ["SUMMARY" (str "Clojure Camp " (:user/name (first guests))
                         " and " (:user/name (last guests)))]
         ["ORGANIZER" "mailto:bot@clojure.camp"]
         ["ATTENDEE" (str "mailto:" (:user/email (first guests)))]
         ["ATTENDEE" (str "mailto:" (:user/email (last guests)))]
         ["UID" id]
         ["DESCRIPTION" (str "Potential topics: " (->topics event))]
         ["LOCATION" (util/->event-url event)]
         ["DTSTART" (format start)]
         ["DTEND" (format end)]
         ["DTSTAMP" (format (.toInstant (java.util.Date.)))]
         ["END" "VEVENT"]
         ["END" "VCALENDAR"]]
        (map (fn [[a b]] (str a ":" b)))
        (string/join "\n"))))

#_(last (db/get-users))
#_(event->ical {:event/guest-ids #{(:user/id (first (db/get-users)))
                                   (:user/id (last (db/get-users)))}
                :event/at #inst "2021-11-08T14:00:00.000-00:00"
                :event/id #uuid "c2492476-8302-4ab0-aee8-abf0039fc09b"})

(defn matched-email-template
  [user-id events]
  (let [get-user (memoize db/get-user)
        user (db/get-user user-id)]
   {:to (:user/email user)
    :subject "Clojure Camp - Your Matches for this Week"
    :attachments (->> events
                      (map (fn [event]
                            {:type :attachment
                             :content-type "text/calendar"
                             :file-name (str "event-" (:event/id event) ".ics")
                             :content (.getBytes (event->ical event))})))
    :body [:div
           [:p "Hi " (:user/name user) ","]
           [:p "Here are your pairing sessions for next week:"]
           (for [event (sort-by :event/at events)
                 :let [partner (get-user (first (disj (:event/guest-ids event) user-id)))]]
            [:p.event
             [:span.datetime
              [:strong
               (.format (ZonedDateTime/ofInstant (.toInstant (:event/at event))
                                                 (ZoneId/of (:user/time-zone user)))
                        (DateTimeFormatter/ofPattern "eee MMM dd 'at' HH:mm"))
               " (" (:user/time-zone user) ")"]]
             [:br]
             "With: "
             [:span.guest
              (:user/name partner)
              " (" (:user/email partner) ")"]
             [:br]
             "Topics: "
             (->topics event)
             [:br]
             [:a {:href (util/->event-url event)} "Meeting Link"]])
           [:p "If you can't make a session, be sure to let your partner know!"]
           [:p "- Clojure Camp Bot"]]}))

#_(email/send! (matched-email-template
                 (:user/id (first (db/get-users)))
                 [{:event/guest-ids #{(:user/id (first (db/get-users)))
                                      (:user/id (last (db/get-users)))}
                   :event/at #inst "2021-11-08T14:00:00.000-00:00"}
                  {:event/guest-ids #{(:user/id (first (db/get-users)))
                                      (:user/id (last (db/get-users)))}
                   :event/at #inst "2021-11-09T14:00:00.000-00:00"}]))

#_(let [[user-id events] (first (group-by-guests (generate-schedule (db/get-users))))]
   (email/send! (sunday-email-template user-id events)))

(defn reset-opt-in! [user-id]
  (-> (db/get-user user-id)
      (assoc :user/pair-next-week? false)
      db/save-user!))

(defn send-sunday-emails! []
  (let [users-to-match (filter :user/pair-next-week? (db/get-users))
        events (generate-schedule users-to-match (LocalDate/now))
        user-id->events (group-by-guests events)
        opted-in-user-ids (set (map :user/id users-to-match))
        matched-user-ids (set (keys user-id->events))
        unmatched-user-ids (set/difference opted-in-user-ids
                                           matched-user-ids)]
   (doseq [event events]
     (p2p.db/save-event! event))

   (doseq [user-id unmatched-user-ids]
     (email/send! (unmatched-email-template user-id))
     (reset-opt-in! user-id))

   (doseq [[user-id events] user-id->events]
     (email/send! (matched-email-template user-id events))
     (reset-opt-in! user-id))))

#_(send-sunday-emails!)

(defn schedule-email-job! []
  (chime/chime-at
    (->> (chime/periodic-seq
           (.adjustInto (LocalTime/of 18 0)
                        (ZonedDateTime/now (ZoneId/of "America/Toronto")))
           (Period/ofDays 1))
         (filter (fn [instant]
                   (= DayOfWeek/SUNDAY (.getDayOfWeek instant)))))
    (fn [_]
     (send-sunday-emails!))))

(api/register-job! ::job schedule-email-job!)

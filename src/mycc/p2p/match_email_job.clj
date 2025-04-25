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
   [mycc.common.date :as date]
   [mycc.p2p.db :as p2p.db]
   [mycc.p2p.meetups :as meetups]
   [modulo.api :as mod])
  (:import
   (java.time Period DayOfWeek ZonedDateTime ZoneId LocalTime LocalDate)
   (java.time.format DateTimeFormatter)
   (java.time.temporal ChronoUnit)))

(defn mapify
  [kf vf coll]
  (zipmap (map kf coll)
          (map vf coll)))

(defn ->inst [zoned-date-time]
  (java.util.Date/from (.toInstant zoned-date-time)))

(defn prep-input-for-schedule
  [users local-date-start-of-week meetup-insts]
  {:times-to-pair 1
   :max-events-per-day (mapify :user/id :user/max-pair-per-day users)
   :max-events-per-week (mapify :user/id :user/max-pair-per-week users)
   :max-same-user-per-week (mapify :user/id :user/max-pair-same-user users)
   :topics (mapify :user/id :user/topics users)
   :timezones (mapify :user/id :user/time-zone users)
   #_#_:roles (-> (mapify :user/id :user/role users)
                  (update-vals (fn [role]
                                 #{role})))
   :primary-languages (mapify :user/id :user/primary-languages users)
   :secondary-languages (mapify :user/id :user/secondary-languages users)
   :user-deny-list (mapify :user/id :user/user-pair-deny-list users)
   #_#_:roles-to-pair-with (mapify :user/id
                                   (fn [user]
                                     (case (:user/role user)
                                       ;; mentors are only paired with students
                                       :role/mentor
                                       {:preferred #{:role/student}
                                        :acceptable #{:role/student}}
                                       ;; students have more choice
                                       :role/student
                                       (case (:user/pair-with user)
                                         :pair-with/only-mentors
                                         {:preferred #{:role/mentor}
                                          :acceptable #{:role/mentor}}
                                         :pair-with/prefer-mentors
                                         {:preferred #{:role/mentor}
                                          :acceptable #{:role/mentor :role/student}}
                                         nil
                                         {:preferred #{:role/mentor :role/student}
                                          :acceptable #{:role/mentor :role/student}}
                                         :pair-with/prefer-students
                                         {:preferred #{:role/student}
                                          :acceptable #{:role/mentor :role/student}}
                                         :pair-with/only-students
                                         {:preferred #{:role/student}
                                          :acceptable #{:role/student}})))
                                   users)
   :availabilities (mapify :user/id
                           ;; stored as {[:monday 10] :available
                           ;;            [:tuesday 10] :preferred
                           ;;            [:wednesday 10] nil)
                           ;; but need #{[#inst "2025-04..." :available]
                           ;;            [#inst "2025-04..." :preferred]}
                           ;; also, remove when value is nil
                           (fn [user]
                             (->> (:user/availability user)
                                  (filter (fn [[_ v]] v))
                                  (map (fn [[k v]]
                                         [(->inst (date/convert-time k (:user/time-zone user) local-date-start-of-week)) v]))
                                  ;; remove times conflicting with scheduled event
                                  (remove (fn [[inst _availability]]
                                            (contains? meetup-insts inst)))
                                  
                                  set))
                           users)})

#_(-> (prep-input-for-schedule (db/get-users) (LocalDate/now) #{#inst "2025-04-29T13:00:00.000-00:00"})
      :availabilities)

(defn generate-schedule
  "Returns a list of maps, with :event/guest-ids, :day-of-week and :Time-of-day,
    ex.
    [{:event/guest-ids #{123 456}
      :event/at #inst \"...\"} ...]"
  [users local-date-start-of-week meetup-insts]
  (if (empty? users)
    []
    (->> (assoc (prep-input-for-schedule users local-date-start-of-week meetup-insts)
                :report-fn println)
         (ps/schedule)
         :schedule
         (map (fn [event]
                (-> event
                    (set/rename-keys {:at :event/at
                                      :guest-ids :event/guest-ids})
                    (assoc :event/id (uuid/random))))))))

#_(generate-schedule (db/get-users) (LocalDate/now) #{})

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
  (let [users (->> (:event/guest-ids event)
                   (map db/get-user))]
    (->> users
         (map (fn [user]
                ;; remove any topics that have nil value
                (update user :user/topics (fn [topics]
                                            (into {} (filter val topics))))))
         (map :user/topics)
         (apply merge-with (fn [a b]
                             [{:user (first users) :level a}
                              {:user (second users) :level b}]))
         ;; the above merge function is not called for non-matching topics,
         ;; so filter for only the matches
         (filter #(vector? (val %)))
         (map (fn [[topic-id levels]]
                {:topic-id topic-id
                 :users-levels levels}))
         ;; [{:topic-id 1234
         ;;   :users-levels [{:user-id 123 :level :expert}
         ;;                  {:user-id 567 :level :beginner}]}]
         ;; shuffle so that ties aren't always in the same order
         shuffle
         (sort-by (fn [{:keys [users-levels]}]
                    ({#{:level/expert :level/intermediate} -4
                      #{:level/intermediate :level/beginner} -3
                      #{:level/intermediate} -2
                      #{:level/expert :level/beginner} -1
                      #{:level/beginner} 0
                      #{:level/expert} 100}
                     (set (map :level users-levels)))))
         (take 3)
         (map (fn [{:keys [topic-id users-levels]}]
                (let [sorted-users-levels (->> users-levels
                                               (sort-by (fn [{:keys [level]}]
                                                          ({:level/beginner 2
                                                            :level/intermediate 1
                                                            :level/expert 0}
                                                           level))))]
                  ;; A (expert) mentors B (beginner) on topic
                  ;; A (intermediate) pairs with B (intermediate) on topic
                  (str (:user/name (:user (first sorted-users-levels)))
                       " ("
                       (name (:level (first sorted-users-levels)))
                       ") "
                       (if (= (:level (first users-levels))
                              (:level (second users-levels)))
                         "pairs with"
                         "mentors")
                       " "
                       (:user/name (:user (second sorted-users-levels)))
                       " ("
                       (name (:level (second sorted-users-levels)))
                       ") "
                       "on '"
                       (:topic/name (p2p.db/get-topic topic-id))
                       "'.")))))))

#_(->topics {:event/guest-ids #{(:user/id (first (db/get-users)))
                                (:user/id (last (db/get-users)))}
             :event/at #inst "2021-11-08T14:00:00.000-00:00"})

(defn unmatched-email-template
  [user-id]
  (let [user (db/get-user user-id)]
    {:to (:user/email user)
     :subject "Your Pairing Sessions this Week (Clojure Camp)"
     :body [:div
            [:p "Hi " (:user/name user) ","]
            [:p "Unfortunately, we couldn't match you with anyone this week."]
            [:p "We're working to add more peers and mentors in your time zone."]
            (when (= :pair-with/only-mentors
                     (:user/pair-with user))
              [:p "You may want to relax your 'Pair With' preference to allow matching with peers, not just mentors."])
            [:p "- Clojure Camp scheduler bot"]]}))

(defn event->ical
  [{:event/keys [guest-ids at id] :as event}]
  (let [guests (map db/get-user guest-ids)
        ;; iCAL expects datetimes in the form: "20211108T140000Z"
        format  (fn [t]
                  (string/replace t #"-|:|(\.\d+)" ""))
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
         ["DESCRIPTION" (str "Potential topics:\\n"
                             (string/join "\\n" (->topics event)))]
         ["LOCATION" (util/->event-url event)]
         ["DTSTART" (format start)]
         ["DTEND" (format end)]
         ["DTSTAMP" (format (.toInstant (java.util.Date.)))]
         ["END" "VEVENT"]
         ["END" "VCALENDAR"]]
        (map (fn [[a b]] (str a ":" b)))
        ;; maximum line length is 75
        (map (fn [s]
               (->> (partition-all 74 s)
                    (map (partial apply str))
                    (string/join "\r\n "))))
        (string/join "\n"))))

#_(last (db/get-users))
#_(event->ical {:event/guest-ids #{(:user/id (first (db/get-users)))
                                   (:user/id (last (db/get-users)))}
                :event/at #inst "2021-11-08T14:00:00.000-00:00"
                :event/id #uuid "c2492476-8302-4ab0-aee8-abf0039fc09b"})

(defn events-for-user-before-inst [user-id inst]
  (->> (p2p.db/get-events-for-user user-id)
       (filter (fn [event]
                 (.before (:event/at event) inst)))))

#_(events-for-user-before-inst (:user/id (first (shuffle (db/get-users))))
                               (java.util.Date.))

(defn matched-email-template
  [user-id events]
  (let [get-user (memoize db/get-user)
        user (db/get-user user-id)]
    {:to (:user/email user)
     :subject "Your Pairing Sessions this Week (Clojure Camp)"
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
               [:a.guest {:href (str (mod/config :app-domain) "/community#" (:user/id partner))}
                (:user/name partner)]
               " (" (:user/email partner) ")"
               ;; This logic may miss events that are scheduled between the job time and the new event time,
               ;; but it is rare and not a big deal
               (let [sessions-count (count (events-for-user-before-inst user-id (java.util.Date.)))]
                 (cond
                   (= sessions-count 0) " (First time pairing!) "
                   (<= sessions-count 3) " (New to pairing) "))
               (when (seq (->topics event))
                 (list [:br]
                       "Potential Topics: "
                       (for [topic (->topics event)]
                         (list [:br] " - " topic))))
               [:br]
               "Where: Discord"
               #_[:a {:href (util/->event-url event)} "Meeting Link"]])
            [:p "If you can't make a session, be sure to let your partner know!"]
            [:p "The default 'location' is Discord, but you can coordinate with your partner and use an alternative with better pairing support like Zoom or Pop.com"]
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
  (let [local-date-start-of-week (LocalDate/now)
        meetup-insts (meetups/all-meetup-insts local-date-start-of-week (mod/config :meetups))
        users-to-match (filter :user/pair-next-week? (db/get-users))
        events (generate-schedule users-to-match local-date-start-of-week meetup-insts)
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

(mod/register-job! ::job schedule-email-job!)

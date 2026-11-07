(ns mycc.pairwithme.email
  (:require
    [clojure.string :as string]
    [modulo.api :as mod]
    [mycc.common.db :as db]
    [mycc.common.ical :as ical]
    [mycc.pairwithme.sessions :as sessions]
    [mycc.pairwithme.slots :as slots])
  (:import
    (java.time.format DateTimeFormatter)
    (java.time.temporal ChronoUnit)))

(defn format-slot-for-user
  [slot user]
  (str (.format (slots/->zoned slot (slots/->zone-id user))
                (DateTimeFormatter/ofPattern "EEE MMM d, HH:mm"))
       " (" (or (:user/time-zone user) "UTC") ")"))

(defn format-slot
  [slot users]
  (->> users
       (map (fn [user]
              (format-slot-for-user slot user)))
       distinct
       (string/join " / ")))

(defn subject
  [initiator recipient]
  (str "Pairing: " (:user/name initiator) " + " (:user/name recipient) " (Clojure Camp)"))

(defn threading-headers
  [earlier-message-ids]
  (when (seq earlier-message-ids)
    {:In-Reply-To (last earlier-message-ids)
     :References (string/join " " earlier-message-ids)}))

(defn session-users
  [session]
  [(db/get-user (:pairwithme-session/initiator-id session))
   (db/get-user (:pairwithme-session/recipient-id session))])

(defn app-url
  [path]
  (str (mod/config :app-domain) path))

(defn proposal-email
  [session]
  (let [[initiator recipient-of-session :as users] (session-users session)
        proposal (sessions/active-proposal session)
        proposer (db/get-user (:proposal/from-user-id proposal))
        recipient (db/get-user (sessions/recipient-id session))
        earlier-message-ids (->> (:pairwithme-session/proposals session)
                                 butlast
                                 (map :proposal/email-message-id))]
    ;; only to the recipient, so the proposer doesn't get the recipient's address before they respond
    {:to [(:user/email recipient)]
     :reply-to (:user/email proposer)
     :message-id (:proposal/email-message-id proposal)
     :headers (threading-headers earlier-message-ids)
     :subject (subject initiator recipient-of-session)
     :body [:div
            [:p "Hi " (:user/name recipient) ","]
            [:p (:user/name proposer)
             (if (seq earlier-message-ids)
               " proposed other times to pair. "
               " would like to pair with you! ")
             "Pick one of these times:"]
            (for [[index slot] (map-indexed vector (:proposal/slots proposal))]
              [:p
               [:strong (format-slot slot users)]
               [:br]
               [:a {:href (app-url (str "/pairwithme/accept/"
                                        (:pairwithme-session/id session) "/"
                                        (:proposal/id proposal) "/"
                                        index))}
                "Accept this time"]])
            [:p "None of these work? "
             [:a {:href (app-url (str "/pairwithme?session=" (:pairwithme-session/id session)))}
              "Propose other times"]]
            (when-let [topics (:user/pairwithme-topics proposer)]
              [:div
               [:p (:user/name proposer) " would like to pair on: " ]
               [:p topics]])
            [:p "To chat with " (:user/name proposer) ", reply to this email."]
            [:p "- Clojure Camp Bot"]]}))

(defn session->ics
  [session users]
  (let [start (.toInstant (:pairwithme-session/accepted-slot session))]
    (ical/->ics
      {:uid (:pairwithme-session/id session)
       :summary (str "Clojure Camp Pairing: "
                     (string/join " and " (map :user/name users)))
       :attendee-emails (mapv :user/email users)
       :description "Pairing session scheduled via my.clojure.camp/pairwithme"
       :location "Discord"
       :start start
       :end (.plus start 1 ChronoUnit/HOURS)})))

(defn accepted-email
  [session]
  (let [[initiator recipient-of-session :as users] (session-users session)
        accepter (db/get-user (:pairwithme-session/accepted-by-user-id session))]
    {:to (mapv :user/email users)
     :reply-to (mapv :user/email users)
     :message-id (sessions/message-id (:pairwithme-session/id session) "accepted")
     :headers (threading-headers (->> (:pairwithme-session/proposals session)
                                      (map :proposal/email-message-id)))
     :subject (subject initiator recipient-of-session)
     :attachments [{:type :attachment
                    :content-type "text/calendar"
                    :file-name (str "pairing-" (:pairwithme-session/id session) ".ics")
                    :content (.getBytes (session->ics session users))}]
     :body [:div
            [:p "Hi " (string/join " and " (map :user/name users)) ","]
            [:p (:user/name accepter) " accepted. You are pairing at:"]
            [:p [:strong (format-slot (:pairwithme-session/accepted-slot session) users)]]
            [:p "The calendar invite is attached."]
            [:p "Where to pair: you can use a Discord room, or a tool with better pairing support, like Tuple (recommended, but no Linux support), Pop.com, or Zoom. Agree with your partner by replying to this email."]
            [:p "If you can't make it, reply to this email to let your partner know."]
            [:p "- Clojure Camp Bot"]]}))

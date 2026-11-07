(ns mycc.pairwithme.sessions
  (:import
    (java.util Date)))

(def slot-duration-ms (* 60 60 1000))

(defn active-proposal
  [session]
  (last (:pairwithme-session/proposals session)))

(defn other-user-id
  [session user-id]
  (if (= user-id (:pairwithme-session/initiator-id session))
    (:pairwithme-session/recipient-id session)
    (:pairwithme-session/initiator-id session)))

(defn recipient-id
  ;; of the active proposal
  [session]
  (other-user-id session (:proposal/from-user-id (active-proposal session))))

(defn accepted?
  [session]
  (some? (:pairwithme-session/accepted-slot session)))

(defn accept-error
  [{:keys [session user-id proposal-id slot-index now]}]
  (let [proposal (->> (:pairwithme-session/proposals session)
                      (filter (fn [proposal]
                                (= proposal-id (:proposal/id proposal))))
                      first)
        slot (get (:proposal/slots proposal) slot-index)]
    (cond
      (or (nil? session)
          (nil? slot))
      :not-found

      (accepted? session)
      :already-accepted

      (not= proposal-id (:proposal/id (active-proposal session)))
      :replaced

      (not= user-id (recipient-id session))
      :wrong-user

      (.before ^Date slot now)
      :past)))

(defn upcoming?
  [session ^Date now]
  (let [slots (if (accepted? session)
                [(:pairwithme-session/accepted-slot session)]
                (:proposal/slots (active-proposal session)))]
    (some (fn [^Date slot]
            (< (.getTime now)
               (+ (.getTime slot) slot-duration-ms)))
          slots)))

(defn status-for
  [session user-id]
  (cond
    (accepted? session)
    :accepted

    (= user-id (recipient-id session))
    :respond

    :else
    :waiting))

(defn message-id
  [session-id suffix]
  (str "<pairwithme-" session-id "-" suffix "@clojure.camp>"))

(defn new-proposal
  [{:keys [session-id from-user-id slots now]}]
  (let [proposal-id (random-uuid)]
    {:proposal/id proposal-id
     :proposal/from-user-id from-user-id
     :proposal/created-at now
     :proposal/slots (vec slots)
     :proposal/email-message-id (message-id session-id proposal-id)}))

(defn new-session
  [{:keys [initiator-id recipient-id slots now]}]
  (let [session-id (random-uuid)]
    {:pairwithme-session/id session-id
     :pairwithme-session/initiator-id initiator-id
     :pairwithme-session/recipient-id recipient-id
     :pairwithme-session/created-at now
     :pairwithme-session/proposals [(new-proposal {:session-id session-id
                                                   :from-user-id initiator-id
                                                   :slots slots
                                                   :now now})]
     :pairwithme-session/accepted-slot nil
     :pairwithme-session/accepted-by-user-id nil
     :pairwithme-session/accepted-at nil}))

(defn add-proposal
  [session {:keys [from-user-id slots now]}]
  (update session :pairwithme-session/proposals conj
          (new-proposal {:session-id (:pairwithme-session/id session)
                         :from-user-id from-user-id
                         :slots slots
                         :now now})))

(defn accept
  [session {:keys [user-id slot now]}]
  (assoc session
    :pairwithme-session/accepted-slot slot
    :pairwithme-session/accepted-by-user-id user-id
    :pairwithme-session/accepted-at now))

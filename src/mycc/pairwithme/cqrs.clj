(ns mycc.pairwithme.cqrs
  (:require
    [modulo.api :as mod]
    [mycc.common.db :as db]
    [mycc.common.email :as email]
    [mycc.pairwithme.db :as pairwithme.db]
    [mycc.pairwithme.email :as pairwithme.email]
    [mycc.pairwithme.schema :as schema]
    [mycc.pairwithme.sessions :as sessions]
    [mycc.pairwithme.slots :as slots]))

(defn user-exists?-condition [user-id]
  [#(db/entity-file-exists? :user user-id) :not-allowed "User with this ID does not exist."])

(defn slots-param? [value]
  (and (sequential? value)
       (every? inst? value)))

(defn can-view-schedule?
  ;; unlisted users are visible only to people they have a session with
  [{:keys [user-id other-user-id]}]
  (or (some-> (db/get-user other-user-id)
              slots/listed?)
      (->> (pairwithme.db/get-sessions-for-user user-id)
           (some (fn [session]
                   (= other-user-id (sessions/other-user-id session user-id)))))))

(defn proposal-recipient-id
  [{:keys [user-id other-user-id session-id]}]
  (if session-id
    (some-> (pairwithme.db/get-session session-id)
            (sessions/other-user-id user-id))
    other-user-id))

(defn valid-slots?
  [{:keys [user-id slots] :as params}]
  (let [now (java.util.Date.)
        proposer (db/get-user user-id)
        recipient (db/get-user (proposal-recipient-id params))]
    (and (<= 1 (count slots) schema/max-slots-per-proposal)
         (apply distinct? slots)
         (every? (fn [slot]
                   (slots/valid-slot? {:now now
                                       :proposer proposer
                                       :recipient recipient
                                       :slot slot}))
                 slots))))

(defn propose!
  [{:keys [user-id other-user-id session-id slots]}]
  (let [now (java.util.Date.)
        session (if session-id
                  (-> (pairwithme.db/get-session session-id)
                      (sessions/add-proposal {:from-user-id user-id
                                              :slots slots
                                              :now now}))
                  (sessions/new-session {:initiator-id user-id
                                         :recipient-id other-user-id
                                         :slots slots
                                         :now now}))]
    (pairwithme.db/save-session! session)
    (email/send! (pairwithme.email/proposal-email session))
    (:pairwithme-session/id session)))

(defn accept!
  [{:keys [user-id session-id proposal-id slot-index]}]
  (let [now (java.util.Date.)
        session (pairwithme.db/get-session session-id)]
    (if-let [error (sessions/accept-error {:session session
                                           :user-id user-id
                                           :proposal-id proposal-id
                                           :slot-index slot-index
                                           :now now})]
      {:error error
       :accepted-slot (:pairwithme-session/accepted-slot session)}
      (let [slot (get (:proposal/slots (sessions/active-proposal session)) slot-index)
            session (sessions/accept session {:user-id user-id
                                              :slot slot
                                              :now now})]
        (pairwithme.db/save-session! session)
        (email/send! (pairwithme.email/accepted-email session))
        {:accepted-slot slot}))))

(defn session-for-client
  [session user-id]
  (let [other-user (db/get-user (sessions/other-user-id session user-id))]
    (assoc session
      :other-user (select-keys other-user [:user/id :user/name])
      :status (sessions/status-for session user-id))))

(def commands
  [{:id :pairwithme/propose!
    :route [:put "/api/pairwithme/propose"]
    :params {:user-id uuid?
             :other-user-id uuid?
             :slots slots-param?}
    :conditions
    (fn [{:keys [user-id other-user-id] :as params}]
      [(user-exists?-condition user-id)
       [#(and (not= user-id other-user-id)
              (some-> (db/get-user other-user-id)
                      slots/listed?))
        :not-allowed "This user is not open to pairing."]
       [#(valid-slots? params)
        :incorrect "Times must be 1-3 different slots when the other person is available, in the next 7 days, at least 12h from now."]])
    :effect propose!
    :return :tada/effect-return}

   {:id :pairwithme/counter-propose!
    :route [:put "/api/pairwithme/counter-propose"]
    :params {:user-id uuid?
             :session-id uuid?
             :slots slots-param?}
    :conditions
    (fn [{:keys [user-id session-id] :as params}]
      [(user-exists?-condition user-id)
       [#(let [session (pairwithme.db/get-session session-id)]
           (and session
                (not (sessions/accepted? session))
                (= user-id (sessions/recipient-id session))))
        :not-allowed "Can't propose times in this session."]
       [#(valid-slots? params)
        :incorrect "Times must be 1-3 different slots when the other person is available, in the next 7 days, at least 12h from now."]])
    :effect propose!
    :return :tada/effect-return}

   ;; errors are returned (not thrown), so the page can show them
   {:id :pairwithme/accept!
    :route [:put "/api/pairwithme/accept"]
    :params {:user-id uuid?
             :session-id uuid?
             :proposal-id uuid?
             :slot-index int?}
    :conditions
    (fn [{:keys [user-id]}]
      [(user-exists?-condition user-id)])
    :effect accept!
    :return :tada/effect-return}])

(def queries
  [{:id :pairwithme/profiles
    :route [:get "/api/pairwithme/profiles"]
    :params {:user-id uuid?}
    :conditions
    (fn [{:keys [user-id]}]
      [(user-exists?-condition user-id)])
    :return
    (fn [{:keys [user-id]}]
      (let [now (java.util.Date.)
            viewer (db/get-user user-id)]
        (->> (db/get-users)
             (filter slots/listed?)
             (remove (fn [user]
                       (= user-id (:user/id user))))
             (sort-by (fn [user]
                        (or (:user/last-visited-at user)
                            (java.util.Date. 0))))
             reverse
             (map (fn [user]
                    (-> user
                        (select-keys [:user/id
                                      :user/name
                                      :user/pairwithme-topics])
                        (assoc :overlap-count (slots/overlap-count {:now now
                                                                    :viewer viewer
                                                                    :other user}))))))))}

   {:id :pairwithme/schedule
    :route [:get "/api/pairwithme/schedule"]
    :params {:user-id uuid?
             :other-user-id uuid?}
    :conditions
    (fn [{:keys [user-id other-user-id] :as params}]
      [(user-exists?-condition user-id)
       (user-exists?-condition other-user-id)
       [#(can-view-schedule? params)
        :not-allowed "This user is not open to pairing."]])
    :return
    (fn [{:keys [user-id other-user-id]}]
      (slots/schedule-grid {:now (java.util.Date.)
                            :viewer (db/get-user user-id)
                            :other (db/get-user other-user-id)}))}

   {:id :pairwithme/sessions
    :route [:get "/api/pairwithme/sessions"]
    :params {:user-id uuid?}
    :conditions
    (fn [{:keys [user-id]}]
      [(user-exists?-condition user-id)])
    :return
    (fn [{:keys [user-id]}]
      (let [now (java.util.Date.)]
        (->> (pairwithme.db/get-sessions-for-user user-id)
             (filter (fn [session]
                       (sessions/upcoming? session now)))
             (sort-by :pairwithme-session/created-at)
             reverse
             (map (fn [session]
                    (session-for-client session user-id))))))}])

(mod/register-cqrs! :pairwithme/commands commands)
(mod/register-cqrs! :pairwithme/queries queries)

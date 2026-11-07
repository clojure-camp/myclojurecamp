(ns mycc.pairwithme.state
  (:require
    [modulo.api :as mod]
    [mycc.pairwithme.schema :as schema]))

;; profiles

(mod/reg-event-fx
  :pairwithme/fetch-profiles!
  (fn [_ _]
    {:ajax {:method :get
            :uri "/api/pairwithme/profiles"
            :on-success (fn [profiles]
                          (mod/dispatch [::store! :pairwithme/profiles profiles]))}}))

(mod/reg-event-fx
  ::store!
  (fn [{db :db} [_ k value]]
    {:db (assoc db k value)}))

(mod/reg-sub
  :pairwithme/profiles
  (fn [db _]
    (db :pairwithme/profiles)))

;; sessions

(mod/reg-event-fx
  :pairwithme/fetch-sessions!
  (fn [_ [_ {:keys [open-counter-for]}]]
    {:ajax {:method :get
            :uri "/api/pairwithme/sessions"
            :on-success (fn [sessions]
                          (mod/dispatch [::store! :pairwithme/sessions sessions])
                          (when open-counter-for
                            (mod/dispatch [::maybe-open-counter! open-counter-for])))}}))

(mod/reg-event-fx
  ::maybe-open-counter!
  (fn [{db :db} [_ session-id]]
    (let [session (->> (db :pairwithme/sessions)
                       (filter (fn [session]
                                 (= session-id (:pairwithme-session/id session))))
                       first)]
      (if (= :respond (:status session))
        {:dispatch [:pairwithme/open-modal! {:other-user (:other-user session)
                                             :session-id session-id}]}
        {:db (assoc db :pairwithme/banner {:type :success
                                           :message "This proposal is no longer open. See your sessions below."})}))))

(mod/reg-sub
  :pairwithme/sessions
  (fn [db _]
    (db :pairwithme/sessions)))

;; modal
;; {:other-user {...} :session-id (optional, for counter-proposals) :selected-slots #{epoch-ms} :schedule [...]}
;; slots are stored as epoch-ms, because js/Date isn't hashed by value

(mod/reg-event-fx
  :pairwithme/open-modal!
  (fn [{db :db} [_ {:keys [other-user session-id]}]]
    {:db (assoc db :pairwithme/modal {:other-user other-user
                                      :session-id session-id
                                      :selected-slots #{}
                                      :schedule nil})
     :ajax {:method :get
            :uri "/api/pairwithme/schedule"
            :params {:other-user-id (:user/id other-user)}
            :on-success (fn [schedule]
                          (mod/dispatch [::store-schedule! schedule]))}}))

(mod/reg-event-fx
  ::store-schedule!
  (fn [{db :db} [_ schedule]]
    {:db (assoc-in db [:pairwithme/modal :schedule] schedule)}))

(mod/reg-event-fx
  :pairwithme/close-modal!
  (fn [{db :db} _]
    {:db (dissoc db :pairwithme/modal)}))

(mod/reg-event-fx
  :pairwithme/toggle-slot!
  (fn [{db :db} [_ slot-ms]]
    (let [selected (get-in db [:pairwithme/modal :selected-slots])]
      (cond
        (contains? selected slot-ms)
        {:db (update-in db [:pairwithme/modal :selected-slots] disj slot-ms)}

        (< (count selected) schema/max-slots-per-proposal)
        {:db (update-in db [:pairwithme/modal :selected-slots] conj slot-ms)}

        :else
        {}))))

(mod/reg-sub
  :pairwithme/modal
  (fn [db _]
    (db :pairwithme/modal)))

(mod/reg-event-fx
  :pairwithme/propose!
  (fn [{db :db} _]
    (let [{:keys [other-user session-id selected-slots]} (db :pairwithme/modal)]
      {:db (assoc-in db [:pairwithme/modal :sending?] true)
       :ajax {:method :put
              :uri (if session-id
                     "/api/pairwithme/counter-propose"
                     "/api/pairwithme/propose")
              :params (merge {:slots (->> selected-slots
                                          sort
                                          (mapv (fn [slot-ms] (js/Date. slot-ms))))}
                             (if session-id
                               {:session-id session-id}
                               {:other-user-id (:user/id other-user)}))
              :on-success (fn [_]
                            (mod/dispatch [::proposal-sent! (:user/name other-user)]))
              :on-error (fn [_]
                          (mod/dispatch [::proposal-failed!]))}})))

(mod/reg-event-fx
  ::proposal-sent!
  (fn [{db :db} [_ other-user-name]]
    {:db (-> db
             (dissoc :pairwithme/modal)
             (assoc :pairwithme/banner {:type :success
                                        :message (str "Proposal sent to " other-user-name " by email. You will get an email when they accept or reply.")}))
     :dispatch [:pairwithme/fetch-sessions!]}))

(mod/reg-event-fx
  ::proposal-failed!
  (fn [{db :db} _]
    {:db (update db :pairwithme/modal assoc
                 :sending? false
                 :error "Could not send. The times may no longer be valid. Close and try again.")}))

;; accept

(mod/reg-event-fx
  :pairwithme/accept!
  (fn [{db :db} [_ {:keys [session-id proposal-id slot-index]}]]
    {:db (assoc db :pairwithme/banner {:type :pending
                                       :message "Accepting..."})
     :ajax {:method :put
            :uri "/api/pairwithme/accept"
            :params {:session-id session-id
                     :proposal-id proposal-id
                     :slot-index slot-index}
            :on-success (fn [result]
                          (mod/dispatch [::accept-result! result]))
            :on-error (fn [_]
                        (mod/dispatch [::accept-result! {:error :not-found}]))}}))

(mod/reg-event-fx
  ::accept-result!
  (fn [{db :db} [_ result]]
    {:db (assoc db :pairwithme/banner (assoc result :type :accept))
     :dispatch [:pairwithme/fetch-sessions!]}))

(mod/reg-sub
  :pairwithme/banner
  (fn [db _]
    (db :pairwithme/banner)))

(mod/reg-event-fx
  :pairwithme/clear-banner!
  (fn [{db :db} _]
    {:db (dissoc db :pairwithme/banner)}))

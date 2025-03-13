(ns mycc.p2p.state
  (:require
    [modulo.api :as mod]
    [mycc.p2p.util :as util]))

(defn key-by [f coll]
  (zipmap (map f coll)
          coll))

(mod/reg-event-fx
  :p2p/fetch-topics!
  (fn [_ _]
    {:ajax {:method :get
            :uri "/api/topics"
            :on-success (fn [topics]
                          (mod/dispatch [::store-topics! topics]))}}))

(mod/reg-event-fx
  ::store-topics!
  (fn [{db :db} [_ topics]]
    {:db (update db :db/topics merge (key-by :topic/id topics))}))

(mod/reg-event-fx
  :p2p/fetch-events!
  (fn [_ _]
    {:ajax {:method :get
            :uri "/api/events"
            :on-success (fn [events]
                          (mod/dispatch [::store-events! events]))}}))

(mod/reg-event-fx
  ::store-events!
  (fn [{db :db} [_ events]]
    {:db (update db :db/events merge (key-by :event/id events))}))

(mod/reg-event-fx
 :clear-availability!
 (fn [{db :db} _]
   {:db (assoc-in db [:db/user :user/availability] {})
    :ajax {:method :put
           :uri "/api/user/clear-availability"}}))

(mod/reg-event-fx
  :set-availability!
  (fn [{db :db} [_ [day hour] value]]
    {:db (assoc-in db [:db/user :user/availability [day hour]] value)
     :ajax {:method :put
            :uri "/api/user/update-availability"
            :params {:day day
                     :hour hour
                     :value value}}}))

(mod/reg-event-fx
 :set-user-topic-level!
 (fn [{db :db} [_ topic-id level]]
   {:db (-> db
            (assoc-in [:db/user :user/topics topic-id] level))
    :ajax {:method :put
           :uri "/api/user/set-topic-level"
           :params {:topic-id topic-id
                    :level level}}}))

(mod/reg-event-fx
  :opt-in-for-pairing!
  (fn [{db :db} [_ bool]]
    {:db (assoc-in db [:db/user :user/pair-next-week?] bool)
     :ajax {:method :put
            :uri "/api/user/opt-in-for-pairing"
            :params {:value bool}}}))

(mod/reg-event-fx
  :avoid-user!
  (fn [{db :db} [_ user-id bool]]
    {:db (update-in db [:db/user :user/user-pair-deny-list] (if bool conj disj) user-id)
     :ajax {:method :put
            :uri "/api/p2p/avoid-user"
            :params {:avoid-user-id user-id
                     :value bool}}}))

(mod/reg-event-fx
  :new-topic!
  (fn [_ [_ topic-name]]
    {:ajax {:method :put
            :uri "/api/topics"
            :params {:name topic-name}
            :on-success (fn [topic]
                          (mod/dispatch [::store-topics! [topic]])
                          (mod/dispatch [:set-user-topic-level! (:topic/id topic) :level/beginner]))}}))

(mod/reg-sub
  :topics
  (fn [db _]
    (vals (db :db/topics))))

(mod/reg-sub
  :events
  (fn [db _]
    (vals (db :db/events))))

;; global availability

(mod/reg-event-fx
  :p2p/fetch-global-availability!
  (fn [_ _]
    {:ajax {:method :get
            :uri "/api/global-availability"
            :on-success (fn [data]
                          (mod/dispatch [::store-global-availability! data]))}}))

(mod/reg-event-fx
  ::store-global-availability!
  (fn [{db :db} [_ data]]
    {:db (assoc db :db/global-availability data)}))

(mod/reg-sub
  :global-availability
  (fn [db [_ date hour]]
    (get-in db [:db/global-availability [date hour]])))

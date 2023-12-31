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
  :set-availability!
  (fn [{db :db} [_ [day hour] value]]
    {:db (assoc-in db [:db/user :user/availability [day hour]] value)
     :ajax {:method :put
            :uri "/api/user/update-availability"
            :params {:day day
                     :hour hour
                     :value value}}}))

(mod/reg-event-fx
  :add-user-topic!
  (fn [{db :db} [_ topic-id]]
    {:db (-> db
             (update-in [:db/user :user/topic-ids] conj topic-id)
             (update-in [:db/topics topic-id :topic/user-count] (fnil inc 0)))
     :ajax {:method :put
            :uri "/api/user/add-topic"
            :params {:topic-id topic-id}}}))

(defn maybe-delete-topic [db topic-id]
  (if (= 0 (get-in db [:db/topics topic-id :topic/user-count]))
   (update db :db/topics dissoc topic-id)
   db))

(mod/reg-event-fx
  :remove-user-topic!
  (fn [{db :db} [_ topic-id]]
    {:db (-> db
             (update-in [:db/user :user/topic-ids] disj topic-id)
             (update-in [:db/topics topic-id :topic/user-count] dec)
             (maybe-delete-topic topic-id))
     :ajax {:method :put
            :uri "/api/user/remove-topic"
            :params {:topic-id topic-id}}}))

(mod/reg-event-fx
  :opt-in-for-pairing!
  (fn [{db :db} [_ bool]]
    {:db (assoc-in db [:db/user :user/pair-next-week?] bool)
     :ajax {:method :put
            :uri "/api/user/opt-in-for-pairing"
            :params {:value bool}}}))

(mod/reg-event-fx
  :update-subscription!
  (fn [{db :db} [_ status]]
    {:db (assoc-in db [:db/user :user/subscribed?] status)
     :ajax {:method :put
            :uri "/api/user/subscription"
            :params {:status status}}}))

(mod/reg-event-fx
  :flag-event-guest!
  (fn [{db :db} [_ event-id value]]
    {:db (update-in db [:db/events event-id] (partial util/flag-other-user value) (get-in db [:db/user :user/id]))
     :ajax {:method :put
            :uri "/api/event/flag-guest"
            :params {:event-id event-id
                     :value value}}}))

(mod/reg-event-fx
  :new-topic!
  (fn [_ [_ topic-name]]
    {:ajax {:method :put
            :uri "/api/topics"
            :params {:name topic-name}
            :on-success (fn [topic]
                          (mod/dispatch [::store-topics! [topic]])
                          (mod/dispatch [:add-user-topic! (:topic/id topic)]))}}))

(mod/reg-sub
  :topics
  (fn [db _]
    (vals (db :db/topics))))

(mod/reg-sub
  :events
  (fn [db _]
    (vals (db :db/events))))


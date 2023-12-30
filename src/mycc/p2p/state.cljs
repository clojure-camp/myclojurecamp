(ns mycc.p2p.state
  (:require
    [mycc.api :as api]
    [mycc.p2p.util :as util]))

(defn key-by [f coll]
  (zipmap (map f coll)
          coll))

(api/reg-event-fx
  :p2p/fetch-topics!
  (fn [_ _]
    {:ajax {:method :get
            :uri "/api/topics"
            :on-success (fn [topics]
                          (api/dispatch [::store-topics! topics]))}}))

(api/reg-event-fx
  ::store-topics!
  (fn [{db :db} [_ topics]]
    {:db (update db :db/topics merge (key-by :topic/id topics))}))

(api/reg-event-fx
  :p2p/fetch-events!
  (fn [_ _]
    {:ajax {:method :get
            :uri "/api/events"
            :on-success (fn [events]
                          (api/dispatch [::store-events! events]))}}))

(api/reg-event-fx
  ::store-events!
  (fn [{db :db} [_ events]]
    {:db (update db :db/events merge (key-by :event/id events))}))

(api/reg-event-fx
  :set-availability!
  (fn [{db :db} [_ [day hour] value]]
    {:db (assoc-in db [:db/user :user/availability [day hour]] value)
     :ajax {:method :put
            :uri "/api/user/update-availability"
            :params {:day day
                     :hour hour
                     :value value}}}))

(api/reg-event-fx
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

(api/reg-event-fx
  :remove-user-topic!
  (fn [{db :db} [_ topic-id]]
    {:db (-> db
             (update-in [:db/user :user/topic-ids] disj topic-id)
             (update-in [:db/topics topic-id :topic/user-count] dec)
             (maybe-delete-topic topic-id))
     :ajax {:method :put
            :uri "/api/user/remove-topic"
            :params {:topic-id topic-id}}}))

(api/reg-event-fx
  :opt-in-for-pairing!
  (fn [{db :db} [_ bool]]
    {:db (assoc-in db [:db/user :user/pair-next-week?] bool)
     :ajax {:method :put
            :uri "/api/user/opt-in-for-pairing"
            :params {:value bool}}}))

(api/reg-event-fx
  :update-subscription!
  (fn [{db :db} [_ status]]
    {:db (assoc-in db [:db/user :user/subscribed?] status)
     :ajax {:method :put
            :uri "/api/user/subscription"
            :params {:status status}}}))

(api/reg-event-fx
  :flag-event-guest!
  (fn [{db :db} [_ event-id value]]
    {:db (update-in db [:db/events event-id] (partial util/flag-other-user value) (get-in db [:db/user :user/id]))
     :ajax {:method :put
            :uri "/api/event/flag-guest"
            :params {:event-id event-id
                     :value value}}}))

(api/reg-event-fx
  :new-topic!
  (fn [_ [_ topic-name]]
    {:ajax {:method :put
            :uri "/api/topics"
            :params {:name topic-name}
            :on-success (fn [topic]
                          (api/dispatch [::store-topics! [topic]])
                          (api/dispatch [:add-user-topic! (:topic/id topic)]))}}))

(api/reg-sub
  :topics
  (fn [db _]
    (vals (db :db/topics))))

(api/reg-sub
  :events
  (fn [db _]
    (vals (db :db/events))))


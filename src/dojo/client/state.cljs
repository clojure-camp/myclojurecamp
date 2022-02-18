(ns dojo.client.state
  (:require
    [bloom.commons.ajax :as ajax]
    [reagent.core :as r]
    [re-frame.core :refer [reg-event-fx reg-fx reg-sub dispatch]]
    [dojo.model :as model]))

(defn key-by [f coll]
  (zipmap (map f coll)
          coll))

(defonce ajax-state (r/atom {}))

(reg-fx :ajax
  (fn [opts]
    (let [request-id (gensym "request")]
     (swap! ajax-state assoc request-id :request.state/in-progress)
     (ajax/request (assoc opts :on-success (fn [data]
                                            (swap! ajax-state dissoc request-id)
                                            (when (opts :on-success)
                                             ((opts :on-success) data))))))))

(reg-event-fx
  :initialize!
  (fn [_ _]
    {:db {:db/topics {}}
     :dispatch-n [[:fetch-user!]]}))

(reg-event-fx
  :fetch-user!
  (fn [_ _]
    {:ajax {:method :get
            :uri "/api/user"
            :on-success (fn [data]
                          (dispatch [::handle-user-data! data])
                          (dispatch [::fetch-other-data!]))}}))

(reg-event-fx
  ::fetch-other-data!
  (fn [_ _]
    {:dispatch-n [[::fetch-topics!]
                  [::fetch-events!]]}))

(reg-event-fx
  ::fetch-topics!
  (fn [_ _]
    {:ajax {:method :get
            :uri "/api/topics"
            :on-success (fn [topics]
                          (dispatch [::store-topics! topics]))}}))

(reg-event-fx
  ::store-topics!
  (fn [{db :db} [_ topics]]
    {:db (update db :db/topics merge (key-by :topic/id topics))}))

(reg-event-fx
  ::fetch-events!
  (fn [_ _]
    {:ajax {:method :get
            :uri "/api/events"
            :on-success (fn [events]
                          (dispatch [::store-events! events]))}}))

(reg-event-fx
  ::store-events!
  (fn [{db :db} [_ events]]
    {:db (update db :db/events merge (key-by :event/id events))}))

(reg-event-fx
  ::maybe-set-time-zone!
  (fn [{db :db} _]
    (when (nil? (get-in db [:db/user :user/time-zone]))
     {:dispatch [:set-user-value! :user/time-zone (.. js/Intl DateTimeFormat resolvedOptions -timeZone)]})))

(reg-event-fx
  :new-topic!
  (fn [_ [_ topic-name]]
    {:ajax {:method :put
            :uri "/api/topics"
            :params {:name topic-name}
            :on-success (fn [topic]
                          (dispatch [::store-topics! [topic]])
                          (dispatch [:add-user-topic! (:topic/id topic)]))}}))

(reg-event-fx
  :log-in!
  (fn [_ [_ email]]
    {:ajax {:method :put
            :uri "/api/request-login-link-email"
            :params {:email email}
            :on-success (fn [data])}}))



(reg-event-fx
  :log-out!
  (fn [_ _]
    {:ajax {:method :delete
            :uri "/api/session"
            :on-success (fn []
                          (dispatch [::remove-user!]))}}))

(reg-event-fx
  ::remove-user!
  (fn [{db :db} _]
    {:db (dissoc db :db/user)}))

(reg-event-fx
  ::handle-user-data!
  (fn [{db :db} [_ data]]
    {:db (assoc db :db/user data)
     :dispatch [::maybe-set-time-zone!]}))

(reg-event-fx
  :set-availability!
  (fn [{db :db} [_ [day hour] value]]
    {:db (assoc-in db [:db/user :user/availability [day hour]] value)
     :ajax {:method :put
            :uri "/api/user/update-availability"
            :params {:day day
                     :hour hour
                     :value value}}}))

(reg-event-fx
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

(reg-event-fx
  :remove-user-topic!
  (fn [{db :db} [_ topic-id]]
    {:db (-> db
             (update-in [:db/user :user/topic-ids] disj topic-id)
             (update-in [:db/topics topic-id :topic/user-count] dec)
             (maybe-delete-topic topic-id))
     :ajax {:method :put
            :uri "/api/user/remove-topic"
            :params {:topic-id topic-id}}}))

(reg-event-fx
  :opt-in-for-pairing!
  (fn [{db :db} [_ bool]]
    {:db (assoc-in db [:db/user :user/pair-next-week?] bool)
     :ajax {:method :put
            :uri "/api/user/opt-in-for-pairing"
            :params {:value bool}}}))

(reg-event-fx
  :set-user-value!
  (fn [{db :db} [_ k v]]
    {:db (assoc-in db [:db/user k] v)
     :ajax {:method :put
            :uri "/api/user/set-profile-value"
            :params {:k k
                     :v v}}}))

(reg-event-fx
  :update-subscription!
  (fn [{db :db} [_ status]]
    {:db (assoc-in db [:db/user :user/subscribed?] status)
     :ajax {:method :put
            :uri "/api/user/subscription"
            :params {:status status}}}))

(reg-event-fx
  :flag-event-guest!
  (fn [{db :db} [_ event-id value]]
    {:db (update-in db [:db/events event-id] (partial model/flag-other-user value) (get-in db [:db/user :user/id]))
     :ajax {:method :put
            :uri "/api/event/flag-guest"
            :params {:event-id event-id
                     :value value}}}))

(reg-sub
  :user
  (fn [db _]
    (db :db/user)))

(reg-sub
  :user-profile-value
  (fn [db [_ k]]
    (get-in db [:db/user k])))

(reg-sub
  :topics
  (fn [db _]
    (vals (db :db/topics))))

(reg-sub
  :events
  (fn [db _]
    (vals (db :db/events))))

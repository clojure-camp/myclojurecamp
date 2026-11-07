(ns mycc.profile.state
  (:require
    [modulo.api :as mod]))

(defn key-by [f coll]
  (zipmap (map f coll)
          coll))

;; topics

(mod/reg-event-fx
  :profile/fetch-topics!
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
 :set-user-topic-level!
 (fn [{db :db} [_ topic-id level]]
   {:db (-> db
            (assoc-in [:db/user :user/topics topic-id] level))
    :ajax {:method :put
           :uri "/api/user/set-topic-level"
           :params {:topic-id topic-id
                    :level level}}}))

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

;; availability

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

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

(def topic-id->selections {#uuid"43fe92c3-1def-4051-aca0-ec4a5b175b0a" {:skill-level "beginner" :session-type "match"}
                           #uuid"76a4323f-960b-4e03-adf3-a3c0d25b8e76" {:skill-level "beginner" :session-type "rally"}
                           #uuid"3e819b1f-705d-4369-863b-b58513288e4a" {:skill-level "expert" :session-type "match"}
                           #uuid"5b5b325c-0611-412a-983b-64efff09578d" {:skill-level "expert" :session-type "rally"}})

#_(def selections->topic-id {"beginner" #{#uuid"43fe92c3-1def-4051-aca0-ec4a5b175b0a" #uuid"76a4323f-960b-4e03-adf3-a3c0d25b8e76"}
                             "expert"   #{#uuid"3e819b1f-705d-4369-863b-b58513288e4a" #uuid"5b5b325c-0611-412a-983b-64efff09578d"}
                             "rally"    #{#uuid"76a4323f-960b-4e03-adf3-a3c0d25b8e76" #uuid"5b5b325c-0611-412a-983b-64efff09578d"}
                             "match"    #{#uuid"3e819b1f-705d-4369-863b-b58513288e4a" #uuid"5b5b325c-0611-412a-983b-64efff09578d"}})

(def selections->topic-id {{:skill-level "beginner" :session-type "match"} #uuid"43fe92c3-1def-4051-aca0-ec4a5b175b0a"
                           {:skill-level "beginner" :session-type "rally"} #uuid"76a4323f-960b-4e03-adf3-a3c0d25b8e76"
                           {:skill-level "expert" :session-type "match"} #uuid"3e819b1f-705d-4369-863b-b58513288e4a"
                           {:skill-level "expert" :session-type "rally"} #uuid"5b5b325c-0611-412a-983b-64efff09578d"})

;(get selections->topic-id {:skill "beginner" :session-type "match"})

;The below format can be used to store selections in :db/user
;{:skill #{"beginner"} :session-type #{"match"}}

(defn topics->selections [user-topic-ids selection-key]
  (set (map (fn [topic-id] (get (get topic-id->selections topic-id) selection-key)) user-topic-ids)))

;TODO - alternate option: based on user selection - always recalculate all the topics - front-end tells server this is the entire list

;can potentially assoc a whole new list of topic ids

(reg-event-fx
  :add-topic-from-skill-level!
  (fn [{db :db} [_ skill-level-item]]
    (let [current-topic-ids-set (get-in db [:db/user :user/topic-ids])
          skill-level-selections (topics->selections current-topic-ids-set :skill-level)
          session-type-selections (topics->selections current-topic-ids-set :session-type)
          topic-ids (map (fn [session-type] (get selections->topic-id {:skill-level skill-level-item :session-type session-type})) session-type-selections)]
      (println topic-ids)
      {:db   (-> db
                 (update-in [:db/user :user/topic-ids] into topic-ids)
                 #_(update-in [:db/topics topic-id :topic/user-count] (fnil inc 0)))
       :ajax {:method :put
              :uri    "/api/user/add-topics"
              :params {:topic-ids topic-ids}}})))

#_(reg-event-fx
    :add-user-topic!
    (fn [{db :db} [_ topic-id]]
      {:db (-> db
               (update-in [:db/user :user/topic-ids] conj topic-id)
               (update-in [:db/topics topic-id :topic/user-count] (fnil inc 0)))
       :ajax {:method :put
              :uri "/api/user/add-topic"
              :params {:topic-id topic-id}}}))

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
    {:db         {:db/checked-auth? false
                  :db/topics        {}
                  :db/skill-level   {:beginner "beginner" :expert "expert"}
                  :db/session-type {:match "match" :rally "rally"}}
     :dispatch-n [[:fetch-user!]]}))

(reg-event-fx
  :fetch-user!
  (fn [_ _]
    {:ajax {:method :get
            :uri "/api/user"
            :on-success (fn [data]
                          (dispatch [::handle-user-data! data])
                          (dispatch [::mark-auth-completed!])
                          (dispatch [::fetch-other-data!]))
            :on-error (fn [_]
                       (dispatch [::mark-auth-completed!]))}}))

(reg-event-fx
  ::mark-auth-completed!
  (fn [{db :db} _]
    {:db (assoc db :db/checked-auth? true)}))

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
             #_(maybe-delete-topic topic-id))
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
  :checked-auth?
  (fn [db _]
    (db :db/checked-auth?)))

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
  :skill-level
  (fn [db _]
    (vals (db :db/skill-level))))

(reg-sub
  :session-type
  (fn [db _]
    (vals (db :db/session-type))))

(reg-sub
  :events
  (fn [db _]
    (vals (db :db/events))))

(reg-sub
  :db
  (fn [db _]
    db))

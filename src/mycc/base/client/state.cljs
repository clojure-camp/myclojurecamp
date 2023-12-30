(ns mycc.base.client.state
  (:require
    [bloom.commons.ajax :as ajax]
    [reagent.core :as r]
    [re-frame.core :refer [reg-event-fx reg-fx reg-sub dispatch]]))

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
    {:db {:db/checked-auth? false
          :db/topics {}}
     :dispatch-n [[:fetch-user!]]}))

(reg-event-fx
  :fetch-user!
  (fn [_ _]
    {:ajax {:method :get
            :uri "/api/user"
            :on-success (fn [data]
                          (dispatch [::handle-user-data! data])
                          (dispatch [::mark-auth-completed!]))
            :on-error (fn [_]
                       (dispatch [::mark-auth-completed!]))}}))

(reg-event-fx
  ::mark-auth-completed!
  (fn [{db :db} _]
    {:db (assoc db :db/checked-auth? true)}))

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
  ::maybe-set-time-zone!
  (fn [{db :db} _]
    (when (nil? (get-in db [:db/user :user/time-zone]))
     {:dispatch [:set-user-value! :user/time-zone (.. js/Intl DateTimeFormat resolvedOptions -timeZone)]})))

(reg-event-fx
  ::handle-user-data!
  (fn [{db :db} [_ data]]
    {:db (assoc db :db/user data)
     :dispatch [::maybe-set-time-zone!]}))

(reg-event-fx
  :set-user-value!
  (fn [{db :db} [_ k v]]
    {:db (assoc-in db [:db/user k] v)
     :ajax {:method :put
            :uri "/api/user/set-profile-value"
            :params {:k k
                     :v v}}}))

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


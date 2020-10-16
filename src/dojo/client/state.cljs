(ns dojo.client.state
  (:require
    [bloom.commons.ajax :as ajax]
    [re-frame.core :refer [reg-event-fx reg-fx reg-sub dispatch]]))

(reg-fx :ajax ajax/request)

(reg-event-fx
  :initialize!
  (fn [_ _]
    {:db {:db/whatever "something"}
     :dispatch-n [[:fetch-user!]]}))

(reg-event-fx
  :fetch-user!
  (fn [_ _]
    {:ajax {:method :get
            :uri "/api/user"
            :on-success (fn [data]
                          (dispatch [::handle-user-data data]))}}))

(reg-event-fx
  :log-in!
  (fn [_ [_ email]]
    {:ajax {:method :put
            :uri "/api/request-login-link-email"
            :params {:email email}
            :on-success (fn [data]

                          )}}))

(reg-event-fx
  ::handle-user-data
  (fn [{db :db} [_ data]]
    {:db (assoc db :db/user data)}))

(reg-event-fx
  :set-availability!
  (fn [{db :db} [_ [day hour] value]]
    {:db (assoc-in db [:db/user :user/availability [day hour]] value)
     :ajax {:method :put
            :uri "/api/user/update-availability"
            :params {:day day
                     :hour hour
                     :value value}}}))

(reg-sub
  :user
  (fn [db _]
    (db :db/user)))

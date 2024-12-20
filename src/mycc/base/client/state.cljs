(ns mycc.base.client.state
  (:require
    [clojure.string :as string]
    [bloom.commons.ajax :as ajax]
    [bloom.omni.fx.dispatch-debounce :as dispatch-debounce]
    [reagent.core :as r]
    [re-frame.core :refer [reg-event-fx reg-fx reg-sub dispatch]]))

(defn key-by [f coll]
  (zipmap (map f coll)
          coll))

(defonce ajax-state (r/atom {}))

(reg-fx :ajax
  (fn [opts]
    (let [request-id (gensym "request")
          ;; only tracking side-effectful ajax
          track? (not= :get (:method opts))]
      (when track?
        (swap! ajax-state assoc request-id :request.state/in-progress))
      (ajax/request (assoc opts
                      :on-success (fn [data]
                                    (when track?
                                      (swap! ajax-state dissoc request-id))
                                    (when (opts :on-success)
                                      ((opts :on-success) data)))
                      :on-error (fn [data]
                                  (when track?
                                    (swap! ajax-state dissoc request-id))
                                  (when (not= :get (:method opts))
                                    (js/alert "Error sending data to server. Your latest changed may not have been saved."))
                                  (when (opts :on-error)
                                    ((opts :on-error) data))))))))


(reg-fx :dispatch-debounce dispatch-debounce/fx)

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
                     :v (if (and (string? v) (string/blank? v))
                          nil
                          v)}}}))

(reg-event-fx
  :debounced-set-user-value!
  (fn [_ [_ k v]]
    {:dispatch-debounce [{:id [:set-user-value! k]
                          :dispatch [:set-user-value! k v]
                          :timeout 500}]}))
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


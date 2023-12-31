(ns mycc.base.cqrs
  (:require
    [clojure.string :as string]
    [mycc.common.db :as db]
    [mycc.common.email :as email]
    [mycc.base.login-link-email :as emails.login-link]
    [modulo.api :as mod]))

(def commands
  [{:id :request-login-link-email!
    :route [:put "/api/request-login-link-email"]
    :params {:email (and string?
                      #(re-matches #".*@.*\..*" %))}
    :effect
    (fn [{:keys [email]}]
      (let [user (or (db/get-user-by-email email)
                     (db/create-user! email))]
        (email/send!
          (emails.login-link/login-email-template user))))}

   (let [validations
         {:user/role #(#{:role/student :role/mentor} %)
          :user/max-pair-per-day #(and (integer? %) (<= 1 % 24))
          :user/max-pair-per-week #(and (integer? %) (<= 1 % (* 24 7)))
          :user/time-zone #(and (string? %)
                             (try
                               (java.time.ZoneId/of %)
                               (catch Exception _
                                 false)))
          :user/name #(and (string? %)
                        (not (string/blank? %)))}]
     {:id :set-profile-value!
      :route [:put "/api/user/set-profile-value"]
      :params {:user-id uuid?
               :k (fn [k]
                    (contains? validations k))
               :v any?}
      :conditions
      (fn [{:keys [user-id k v]}]
        [[#(db/entity-file-exists? :user user-id) :not-allowed "User with this ID does not exist."]
         [#((validations k) v)
          :not-allowed
          "Value is of wrong type."]])
      :effect
      (fn [{:keys [user-id k v]}]
        (some-> (db/get-user user-id)
                (assoc k v)
                db/save-user!))})

   {:id :update-subscription!
    :route [:put "/api/user/subscription"]
    :params {:user-id uuid?
             :status boolean?}
    :conditions
    (fn [{:keys [user-id]}]
      [[#(db/entity-file-exists? :user user-id) :not-allowed "User with this ID does not exist."]])
    :effect
    (fn [{:keys [user-id status]}]
      (some-> (db/get-user user-id)
              (assoc :user/subscribed? status)
              db/save-user!))}])

(def queries
  [{:id :user
    :route [:get "/api/user"]
    :params {:user-id uuid?}
    :conditions
    (fn [{:keys [user-id]}]
      [[#(db/entity-file-exists? :user user-id) :not-allowed "User with this ID does not exist."]])
    :return
    (fn [{:keys [user-id]}]
      (db/get-user user-id))}])

(mod/register-cqrs! :base/commands commands)
(mod/register-cqrs! :base/queries queries)

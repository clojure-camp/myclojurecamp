(ns mycc.base.cqrs
  (:require
    [mycc.common.db :as db]
    [mycc.common.email :as email]
    [mycc.base.login-link-email :as emails.login-link]
    [modulo.api :as mod]
    [mycc.base.schema :as schema]
    [mycc.common.return-to :as return-to]))

(def commands
  [{:id :request-login-link-email!
    :route [:put "/api/request-login-link-email"]
    :params {:email (and string?
                      #(re-matches #".*@.*\..*" %))
             :return-to (fn [value]
                          (or (nil? value)
                              (return-to/safe? value)))}
    :effect
    (fn [{:keys [email return-to]}]
      (let [user (or (db/get-user-by-email email)
                     (db/create-user! email))]
        (email/send!
          (emails.login-link/login-email-template user return-to))))}

   {:id :set-profile-value!
    :route [:put "/api/user/set-profile-value"]
    :params {:user-id uuid?
             :k (fn [k]
                  (schema/allowed-key? schema/User k))
             :v any?}
    :conditions
    (fn [{:keys [user-id k v]}]
      [[#(db/entity-file-exists? :user user-id) :not-allowed "User with this ID does not exist."]
       [#(schema/valid-key-value? schema/User k v)
        :not-allowed
        "Value is of wrong type."]])
    :effect
    (fn [{:keys [user-id k v]}]
      (some-> (db/get-user user-id)
              (assoc k v)
              db/save-user!))}

   {:id :mark-visited!
    :route [:put "/api/user/visited"]
    :params {:user-id uuid?}
    :conditions
    (fn [{:keys [user-id]}]
      [[#(db/entity-file-exists? :user user-id) :not-allowed "User with this ID does not exist."]])
    :effect
    (fn [{:keys [user-id]}]
      (some-> (db/get-user user-id)
              (assoc :user/last-visited-at (java.util.Date.))
              db/save-user!))}

   ;; if we leak user ids, this can be abused to force unsubscribe
   ;; would be better to use an encryped value or hmac
   {:id :email-unsubscribe!
    :route [:post "/api/p2p/unsubscribe"]
    ;; uid coming via url param
    :params {:uid uuid?}
    :conditions
    (fn [{:keys [user-id]}]
      [[#(db/entity-file-exists? :user user-id) :not-allowed "User with this ID does not exist."]])
    :effect
    (fn [{:keys [user-id]}]
      (-> (db/get-user user-id)
          (assoc :user/subscribed? false)
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

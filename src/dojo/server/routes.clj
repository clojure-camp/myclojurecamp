(ns dojo.server.routes
  (:require
    [clojure.string :as string]
    [tada.events.core :as tada]
    [tada.events.ring]
    [dojo.db :as db]
    [dojo.email :as email]
    [dojo.emails.login-link :as emails.login-link]))

(def commands
  [{:id :request-login-link-email!
    :params {:email (and string?
                         #(re-matches #".*@.*\..*" %))}
    :effect
    (fn [{:keys [email]}]
      (let [user (or (db/get-user-by-email email)
                     (db/create-user! email))]
        (email/send!
          (emails.login-link/login-email-template user))))}

   {:id :create-topic!
    :params {:user-id uuid?
             :name (and string? (complement string/blank?))}
    :conditions
    (fn [{:keys [user-id name]}]
      [[#(db/user-exists? user-id) :not-allowed "User with this ID does not exist."]
       [#(not (db/topic-name-exists? name)) :not-allowed "Topic with this name already exists."]])
    :effect
    (fn [{:keys [name]}]
      (db/create-topic! name))
    :return :tada/effect-return}])

#_(tada/do :request-login-link-email! {:email "foo@example.com"})

(def queries
  [{:id :user
    :params {:user-id uuid?}
    :conditions
    (fn [{:keys [user-id]}]
      [[#(db/user-exists? user-id) :not-allowed "User with this ID does not exist."]])
    :return
    (fn [{:keys [user-id]}]
      (db/get-user user-id))}])

(tada/register! commands)
(tada/register! queries)

(defn params-middleware [handler]
  (fn [request]
    ;; TADA wants a :params key on requests
    (handler (assoc request :params
              (assoc (:body-params request)
                     :user-id (get-in request [:session :user-id]))))))

(def routes
  [
   [[:put "/api/request-login-link-email"]
    (tada.events.ring/route :request-login-link-email!)
    [params-middleware]]

   [[:get "/api/user"]
    (tada.events.ring/route :user)
    [params-middleware]]

   [[:get "/api/topics"]
    (fn [_]
      {:body (db/get-topics)})]

   [[:put "/api/topics"]
    (tada.events.ring/route :create-topic!)
    [params-middleware]]

   [[:put "/api/user/add-topic"]
    (fn [request]
      (some-> (db/get-user (get-in request [:session :user-id]))
              (update :user/topic-ids conj (get-in request [:body-params :topic-id]))
              db/save-user!)
      {:status 200})]

   [[:put "/api/user/remove-topic"]
    (fn [request]
      (some-> (db/get-user (get-in request [:session :user-id]))
              (update :user/topic-ids disj (get-in request [:body-params :topic-id]))
              db/save-user!)
      {:status 200})]

   [[:put "/api/user/update-availability"]
    (fn [request]
      (let [{:keys [hour day value]} (request :body-params)]
        (some-> (db/get-user (get-in request [:session :user-id]))
                (assoc-in [:user/availability [day hour]] value)
                db/save-user!))
      {:status 200})]

   [[:put "/api/user/opt-in-for-pairing"]
    (fn [request]
      (let [{:keys [value]} (request :body-params)]
        (some-> (db/get-user (get-in request [:session :user-id]))
                (assoc :user/pair-next-week? value)
                db/save-user!))
      {:status 200})]

   [[:put "/api/user/set-profile-value"]
    (fn [request]
      (if (not (contains? #{:user/max-pair-per-day
                            :user/max-pair-per-week
                            :user/name}
                          (get-in request [:body-params :k])))
        {:status 400}
        (let [{:keys [k v]} (request :body-params)]
          (some-> (db/get-user (get-in request [:session :user-id]))
                  (assoc k v)
                  db/save-user!)
          {:status 200})))]

   [[:delete "/api/session"]
    (fn [request]
      {:status 200
       :session nil})]])

(ns dojo.server.routes
  (:require
    [clojure.string :as string]
    [tada.events.core :as tada]
    [tada.events.ring]
    [dojo.db :as db]
    [dojo.email :as email]
    [dojo.emails.login-link :as emails.login-link]
    [dojo.model :as model]))

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
      [[#(db/exists? :user user-id) :not-allowed "User with this ID does not exist."]
       [#(not (db/topic-name-exists? name)) :not-allowed "Topic with this name already exists."]])
    :effect
    (fn [{:keys [name]}]
      (db/create-topic! name))
    :return :tada/effect-return}

   {:id :subscribe-to-topic!
    :params {:user-id uuid?
             :topic-id uuid?}
    :conditions
    (fn [{:keys [user-id topic-id]}]
      [[#(db/exists? :user user-id) :not-allowed "User with this ID does not exist."]
       [#(db/exists? :topic topic-id) :not-allowed "Topic with this ID does not exist."]])
    :effect
    (fn [{:keys [user-id topic-id]}]
      (some-> (db/get-user user-id)
              (update :user/topic-ids conj topic-id)
              db/save-user!))}

   {:id :unsubscribe-from-topic!
    :params {:user-id uuid?
             :topic-id uuid?}
    :conditions
    (fn [{:keys [user-id topic-id]}]
      [[#(db/exists? :user user-id) :not-allowed "User with this ID does not exist."]
       [#(db/exists? :topic topic-id) :not-allowed "Topic with this ID does not exist."]])
    :effect
    (fn [{:keys [user-id topic-id]}]
      (some-> (db/get-user user-id)
              (update :user/topic-ids disj topic-id)
              db/save-user!))}

   {:id :update-availability!
    :params {:user-id uuid?
             :hour (fn [h] (contains? (set model/hours) h))
             :day (fn [d] (contains? (set model/days) d))
             :value (fn [v] (contains? model/availability-values v))}
    :conditions
    (fn [{:keys [user-id]}]
      [[#(db/exists? :user user-id) :not-allowed "User with this ID does not exist."]])
    :effect
    (fn [{:keys [user-id day hour value]}]
      (some-> (db/get-user user-id)
              (assoc-in [:user/availability [day hour]] value)
              db/save-user!))}

   {:id :opt-in-for-pairing!
    :params {:user-id uuid?
             :value boolean?}
    :conditions
    (fn [{:keys [user-id]}]
      [[#(db/exists? :user user-id) :not-allowed "User with this ID does not exist."]])
    :effect
    (fn [{:keys [user-id value]}]
      (some-> (db/get-user user-id)
              (assoc :user/pair-next-week? value)
              db/save-user!))}

   {:id :set-profile-value!
    :params {:user-id uuid?
             :k (fn [k]
                  (contains? #{:user/max-pair-per-day
                               :user/max-pair-per-week
                               :user/name}
                             k))
             :v any?}
    :conditions
    (fn [{:keys [user-id k v]}]
      [[#(db/exists? :user user-id) :not-allowed "User with this ID does not exist."]
       [#(case k
           :user/max-pair-per-day (and (integer? v) (< 0 v 24))
           :user/max-pair-per-week (and (integer? v) (< 0 v (* 24 7)))
           :user/name (and (string? v)
                           (not (string/blank? v))))
        :not-allowed
        "Value is of wrong type."]])
    :effect
    (fn [{:keys [user-id k v]}]
      (some-> (db/get-user user-id)
              (assoc k v)
              db/save-user!))}])

#_(tada/do :request-login-link-email! {:email "foo@example.com"})

(def queries
  [{:id :user
    :params {:user-id uuid?}
    :conditions
    (fn [{:keys [user-id]}]
      [[#(db/exists? :user user-id) :not-allowed "User with this ID does not exist."]])
    :return
    (fn [{:keys [user-id]}]
      (db/get-user user-id))}

   {:id :topics
    :params {:user-id uuid?}
    :conditions
    (fn [{:keys [user-id]}]
      [[#(db/exists? :user user-id) :not-allowed "User with this ID does not exist."]])
    :return
    (fn [_]
      (db/get-topics))}])

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
    (tada.events.ring/route :topics)
    [params-middleware]]

   [[:put "/api/topics"]
    (tada.events.ring/route :create-topic!)
    [params-middleware]]

   [[:put "/api/user/add-topic"]
    (tada.events.ring/route :subscribe-to-topic!)
    [params-middleware]]

   [[:put "/api/user/remove-topic"]
    (tada.events.ring/route :unsubscribe-from-topic!)
    [params-middleware]]

   [[:put "/api/user/update-availability"]
    (tada.events.ring/route :update-availability!)
    [params-middleware]]

   [[:put "/api/user/opt-in-for-pairing"]
    (tada.events.ring/route :opt-in-for-pairing!)
    [params-middleware]]

   [[:put "/api/user/set-profile-value"]
    (tada.events.ring/route :set-profile-value!)
    [params-middleware]]

   [[:delete "/api/session"]
    (fn [_]
      {:status 200
       :session nil})]])

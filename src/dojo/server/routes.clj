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
    :route [:put "/api/request-login-link-email"]
    :params {:email (and string?
                         #(re-matches #".*@.*\..*" %))}
    :effect
    (fn [{:keys [email]}]
      (let [user (or (db/get-user-by-email email)
                     (db/create-user! email))]
        (email/send!
          (emails.login-link/login-email-template user))))}

   {:id :create-topic!
    :route [:put "/api/topics"]
    :params {:user-id uuid?
             :name (and string? (complement string/blank?))}
    :conditions
    (fn [{:keys [user-id name]}]
      [[#(db/entity-file-exists? :user user-id) :not-allowed "User with this ID does not exist."]
       [#(not (db/topic-name-exists? name)) :not-allowed "Topic with this name already exists."]])
    :effect
    (fn [{:keys [name]}]
      (db/create-topic! name))
    :return :tada/effect-return}

   {:id :subscribe-to-topic!
    :route [:put "/api/user/add-topic"]
    :params {:user-id uuid?
             :topic-id uuid?}
    :conditions
    (fn [{:keys [user-id topic-id]}]
      [[#(db/entity-file-exists? :user user-id) :not-allowed "User with this ID does not exist."]
       [#(db/entity-file-exists? :topic topic-id) :not-allowed "Topic with this ID does not exist."]])
    :effect
    (fn [{:keys [user-id topic-id]}]
      (some-> (db/get-user user-id)
              (update :user/topic-ids conj topic-id)
              db/save-user!))}

   {:id :unsubscribe-from-topic!
    :route [:put "/api/user/remove-topic"]
    :params {:user-id uuid?
             :topic-id uuid?}
    :conditions
    (fn [{:keys [user-id topic-id]}]
      [[#(db/entity-file-exists? :user user-id) :not-allowed "User with this ID does not exist."]
       [#(db/entity-file-exists? :topic topic-id) :not-allowed "Topic with this ID does not exist."]])
    :effect
    (fn [{:keys [user-id topic-id]}]
      (some-> (db/get-user user-id)
              (update :user/topic-ids disj topic-id)
              db/save-user!)
      ;; delete topic if has 0 users
      (->> (db/get-topics)
           (filter (fn [topic] (and (= 0 (:topic/user-count topic))
                                    (= (:topic/id topic) topic-id))))
           (map (fn [topic]
                  (db/delete-topic! (:topic/id topic))))
           (dorun)))}

   {:id :update-availability!
    :route [:put "/api/user/update-availability"]
    :params {:user-id uuid?
             :hour (fn [h] (contains? (set model/hours) h))
             :day (fn [d] (contains? (set model/days) d))
             :value (fn [v] (contains? model/availability-values v))}
    :conditions
    (fn [{:keys [user-id]}]
      [[#(db/entity-file-exists? :user user-id) :not-allowed "User with this ID does not exist."]])
    :effect
    (fn [{:keys [user-id day hour value]}]
      (some-> (db/get-user user-id)
              (assoc-in [:user/availability [day hour]] value)
              db/save-user!))}

   {:id :opt-in-for-pairing!
    :route [:put "/api/user/opt-in-for-pairing"]
    :params {:user-id uuid?
             :value boolean?}
    :conditions
    (fn [{:keys [user-id]}]
      [[#(db/entity-file-exists? :user user-id) :not-allowed "User with this ID does not exist."]])
    :effect
    (fn [{:keys [user-id value]}]
      (some-> (db/get-user user-id)
              (assoc :user/pair-next-week? value)
              db/save-user!))}

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
              db/save-user!))}

   {:id :flag-user!
    :route [:put "/api/event/flag-guest"]
    :params {:user-id uuid?
             :event-id uuid?
             :value boolean?}
    :conditions
    (fn [{:keys [user-id event-id]}]
      [[#(db/entity-file-exists? :user user-id) :not-allowed "User with this ID does not exist."]
       [#(db/entity-file-exists? :event event-id) :not-allowed "Event with this ID does not exist."]])
    :effect
    (fn [{:keys [user-id event-id value]}]
      (some-> (db/get-event event-id)
              ((partial model/flag-other-user value) user-id)
              db/save-event!))}])

#_(tada/do :request-login-link-email! {:email "foo@example.com"})

(def queries
  [{:id :user
    :route [:get "/api/user"]
    :params {:user-id uuid?}
    :conditions
    (fn [{:keys [user-id]}]
      [[#(db/entity-file-exists? :user user-id) :not-allowed "User with this ID does not exist."]])
    :return
    (fn [{:keys [user-id]}]
      (db/get-user user-id))}

   {:id :topics
    :route [:get "/api/topics"]
    :params {:user-id uuid?}
    :conditions
    (fn [{:keys [user-id]}]
      [[#(db/entity-file-exists? :user user-id) :not-allowed "User with this ID does not exist."]])
    :return
    (fn [_]
      (db/get-topics))}

   {:id :events
    :route [:get "/api/events"]
    :params {:user-id uuid?}
    :conditions
    (fn [{:keys [user-id]}]
      [[#(db/entity-file-exists? :user user-id) :not-allowed "User with this ID does not exist."]])
    :return
    (fn [{:keys [user-id]}]
      (->> (db/get-events-for-user user-id)
           ;; enhance event objects with extra info the client needs
           (map (fn [event]
                 (assoc event :event/other-guest
                   (-> (:event/guest-ids event)
                       (disj user-id)
                       first
                       db/get-user
                       (select-keys [:user/id :user/name :user/email])))))))}])

(tada/register! (concat commands queries))

(defn params-middleware [handler]
  (fn [request]
    ;; TADA wants a :params key on requests
    (handler (assoc request :params
              (assoc (:body-params request)
                     :user-id (get-in request [:session :user-id]))))))

(def routes
  (concat
   (->> (concat commands queries)
        (map (fn [{:keys [id route]}]
               [route
                (tada.events.ring/route id)
                [params-middleware]])))

   [[[:get "/validate"]
     (fn [request]
       ;; NOTE: a GET request with a side effect
       ;; first time after registering, users get sent here
       (some-> (get-in request [:session :user-id])
               (db/get-user)
               (assoc :user/email-validated? true)
               db/save-user!)
       {:status 302
        :headers {"Location" "/"}})]]

   [[[:delete "/api/session"]
     (fn [_]
      {:status 200
       :session nil})]]))

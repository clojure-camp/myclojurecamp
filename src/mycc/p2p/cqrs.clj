(ns mycc.p2p.cqrs
  (:require
    [clojure.string :as string]
    [mycc.common.db :as db]
    [mycc.p2p.db :as p2p.db]
    [mycc.p2p.util :as util]
    [mycc.common.date :as date]
    [modulo.api :as mod]))

(def commands
  [{:id :create-topic!
    :route [:put "/api/topics"]
    :params {:user-id uuid?
             :name (and string? (complement string/blank?))}
    :conditions
    (fn [{:keys [user-id name]}]
      [[#(db/entity-file-exists? :user user-id) :not-allowed "User with this ID does not exist."]
       [#(not (p2p.db/topic-name-exists? name)) :not-allowed "Topic with this name already exists."]])
    :effect
    (fn [{:keys [name]}]
      (p2p.db/create-topic! name))
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
      #_(->> (p2p.db/get-topics)
           (filter (fn [topic] (and (= 0 (:topic/user-count topic))
                                 (= (:topic/id topic) topic-id))))
           (map (fn [topic]
                  (p2p.db/delete-topic! (:topic/id topic))))
           (dorun)))}

   {:id :update-availability!
    :route [:put "/api/user/update-availability"]
    :params {:user-id uuid?
             :hour (fn [h] (contains? (set (concat util/early-hours util/hours util/late-hours)) h))
             :day (fn [d] (contains? (set util/days) d))
             :value (fn [v] (contains? util/availability-values v))}
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
              (update :user/pair-opt-in-history (if value conj disj)
                      (date/next-monday))
              db/save-user!))}

   {:id :avoid-user!
    :route [:put "/api/p2p/avoid-user"]
    :params {:user-id uuid?
             :avoid-user-id uuid?
             :value boolean?}
    :conditions
    (fn [{:keys [user-id avoid-user-id]}]
      [[#(db/entity-file-exists? :user user-id) :not-allowed "User with this ID does not exist."]
       [#(db/entity-file-exists? :user avoid-user-id) :not-allowed "Other user with this ID does not exist."]])
    :effect
    (fn [{:keys [user-id avoid-user-id value]}]
      (some-> (db/get-user user-id)
              (update :user/user-pair-deny-list
                      (if value conj disj) avoid-user-id)
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
  [{:id :topics
    :route [:get "/api/topics"]
    :params {:user-id uuid?}
    :conditions
    (fn [{:keys [user-id]}]
      [[#(db/entity-file-exists? :user user-id) :not-allowed "User with this ID does not exist."]])
    :return
    (fn [_]
      (p2p.db/get-topics))}

   {:id :events
    :route [:get "/api/events"]
    :params {:user-id uuid?}
    :conditions
    (fn [{:keys [user-id]}]
      [[#(db/entity-file-exists? :user user-id) :not-allowed "User with this ID does not exist."]])
    :return
    (fn [{:keys [user-id]}]
      (->> (p2p.db/get-events-for-user user-id)
           ;; enhance event objects with extra info the client needs
           (map (fn [event]
                 (assoc event :event/other-guest
                   (-> (:event/guest-ids event)
                       (disj user-id)
                       first
                       db/get-user
                       (select-keys [:user/id :user/name :user/email])))))))} ])

(mod/register-cqrs! :p2p/commands commands)
(mod/register-cqrs! :p2p/queries queries)

(ns mycc.p2p.cqrs
  (:require
    [clojure.string :as string]
    [mycc.common.db :as db]
    [mycc.p2p.db :as p2p.db]
    [mycc.p2p.util :as util]
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
      (->> (p2p.db/get-topics)
           (filter (fn [topic] (and (= 0 (:topic/user-count topic))
                                 (= (:topic/id topic) topic-id))))
           (map (fn [topic]
                  (p2p.db/delete-topic! (:topic/id topic))))
           (dorun)))}

   {:id :update-availability!
    :route [:put "/api/user/update-availability"]
    :params {:user-id uuid?
             :hour (fn [h] (contains? (set util/hours) h))
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
      (some-> (p2p.db/get-event event-id)
              ((partial util/flag-other-user value) user-id)
              p2p.db/save-event!))}])

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

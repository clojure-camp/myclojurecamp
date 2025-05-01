(ns mycc.p2p.cqrs
  (:require
    [clojure.string :as string]
    [mycc.common.db :as db]
    [mycc.p2p.availability :as p2p.availability]
    [mycc.p2p.meetups :as p2p.meetups]
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

   {:id :set-topic-level!
    :route [:put "/api/user/set-topic-level"]
    :params {:user-id uuid?
             :topic-id uuid?
             :level (fn [value]
                      (contains? #{:level/beginner :level/intermediate :level/expert nil} value))}
    :conditions
    (fn [{:keys [user-id topic-id level]}]
      [[#(db/entity-file-exists? :user user-id) :not-allowed "User with this ID does not exist."]
       [#(db/entity-file-exists? :topic topic-id) :not-allowed "Topic with this ID does not exist."]])
    :effect
    (fn [{:keys [user-id topic-id level]}]
      (some-> (db/get-user user-id)
              (assoc-in [:user/topics topic-id] level)
              db/save-user!))}

   {:id :clear-availability!
    :route [:put "/api/user/clear-availability"]
    :params {:user-id uuid?}
    :conditions
    (fn [{:keys [user-id]}]
      [[#(db/entity-file-exists? :user user-id) :not-allowed "User with this ID does not exist."]])
    :effect
    (fn [{:keys [user-id]}]
      (some-> (db/get-user user-id)
              (assoc-in [:user/availability] {})
              db/save-user!))}

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

   {:id :all-user-availability
    :route [:get "/api/global-availability"]
    :params {:user-id uuid?}
    :conditions
    (fn [{:keys [user-id]}]
      [[#(db/entity-file-exists? :user user-id) :not-allowed "User with this ID does not exist."]])
    :return
    (fn [{:keys [user-id]}]
      (p2p.availability/global-availability user-id))}

   {:id :next-week-meetups-in-user-time-zone
    :route [:get "/api/p2p/next-week-meetups-in-user-time-zone"]
    :params {:user-id uuid?}
    :conditions
    (fn [{:keys [user-id]}]
      [[#(db/entity-file-exists? :user user-id) :not-allowed "User with this ID does not exist."]])
    :return
    (fn [{:keys [user-id]}]
      (p2p.meetups/next-week-meetups-in-local-time
       (:user/time-zone (db/get-user user-id))
       (mod/config :meetups)))}

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

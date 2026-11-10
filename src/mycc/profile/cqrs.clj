(ns mycc.profile.cqrs
  (:require
    [clojure.string :as string]
    [modulo.api :as mod]
    [mycc.common.db :as db]
    [mycc.p2p.util :as util]
    [mycc.profile.db :as profile.db]))

(def commands
  [{:id :create-topic!
    :route [:put "/api/topics"]
    :params {:user-id :uuid
             :name [:and :string [:fn (complement string/blank?)]]}
    :conditions
    (fn [{:keys [user-id name]}]
      [[#(db/entity-file-exists? :user user-id) :not-allowed "User with this ID does not exist."]
       [#(not (profile.db/topic-name-exists? name)) :not-allowed "Topic with this name already exists."]])
    :effect
    (fn [{:keys [name]}]
      (profile.db/create-topic! name))
    :return :tada/effect-return}

   {:id :set-topic-level!
    :route [:put "/api/user/set-topic-level"]
    :params {:user-id :uuid
             :topic-id :uuid
             :level [:maybe [:enum :level/beginner :level/intermediate :level/expert]]}
    :conditions
    (fn [{:keys [user-id topic-id]}]
      [[#(db/entity-file-exists? :user user-id) :not-allowed "User with this ID does not exist."]
       [#(db/entity-file-exists? :topic topic-id) :not-allowed "Topic with this ID does not exist."]])
    :effect
    (fn [{:keys [user-id topic-id level]}]
      (some-> (db/get-user user-id)
              (assoc-in [:user/topics topic-id] level)
              db/save-user!))}

   {:id :clear-availability!
    :route [:put "/api/user/clear-availability"]
    :params {:user-id :uuid}
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
    :params {:user-id :uuid
             :hour (into [:enum] (concat util/early-hours util/hours util/late-hours))
             :day (into [:enum] util/days)
             :value [:maybe (into [:enum] (remove nil? util/availability-values))]}
    :conditions
    (fn [{:keys [user-id]}]
      [[#(db/entity-file-exists? :user user-id) :not-allowed "User with this ID does not exist."]])
    :effect
    (fn [{:keys [user-id day hour value]}]
      (some-> (db/get-user user-id)
              (assoc-in [:user/availability [day hour]] value)
              db/save-user!))}])

(def queries
  [{:id :topics
    :route [:get "/api/topics"]
    :params {:user-id :uuid}
    :conditions
    (fn [{:keys [user-id]}]
      [[#(db/entity-file-exists? :user user-id) :not-allowed "User with this ID does not exist."]])
    :return
    (fn [_]
      (profile.db/get-topics))}])

(mod/register-cqrs! :profile/commands commands)
(mod/register-cqrs! :profile/queries queries)

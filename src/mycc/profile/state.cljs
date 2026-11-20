(ns mycc.profile.state
  (:require
    [modulo.api :as mod]
    [mycc.base.client.state :as state]))

;; topics

(defn set-user-topic-level! [topic-id level]
  (swap! state/user assoc-in [:user/topics topic-id] level)
  (mod/tada! [:set-topic-level! {:topic-id topic-id
                                 :level level}]))

(defn new-topic! [topic-name]
  (-> (mod/tada! [:create-topic! {:name topic-name}])
      (.then (fn [topic]
               (set-user-topic-level! (:topic/id topic) :level/beginner)
               topic))))

;; availability

(defn clear-availability! []
  (swap! state/user assoc :user/availability {})
  (mod/tada! [:clear-availability!]))

(defn set-availability! [[day hour] value]
  (swap! state/user assoc-in [:user/availability [day hour]] value)
  (mod/tada! [:update-availability! {:day day
                                     :hour hour
                                     :value value}]))

(ns mycc.p2p.db
  (:require
    [bloom.commons.uuid :as uuid]
    [clojure.java.io :as java.io]
    [mycc.common.db :as db]))

;; topic

(defn get-topic
  "Return topic matching topic-id (without :topic/user-count)"
  [topic-id]
  (when topic-id
    (db/parse-file (db/->path :topic topic-id))))

(defn- user-topic-frequencies
  "Returns a map of topic-id->user-count"
  []
  (->> (db/get-users)
       (mapcat :user/topic-ids)
       frequencies))

(defn- get-topics-raw
  "Return topics (without :topic/user-count)"
  []
  (db/get-entities :topic))

(defn get-topics
  "Return topics, with :topic/user-count"
  []
  (let [topic-id->count (user-topic-frequencies)]
    (->> (get-topics-raw)
         (map (fn [topic]
                (assoc topic :topic/user-count
                  (or (topic-id->count (:topic/id topic))
                      0)))))))

(defn topic-name-exists?
  "Returns true if any topic already has name"
  [name]
  (->> (get-topics)
       (some (fn [topic]
               (= (:topic/name topic) name)))))

(defn save-topic!
  [topic]
  (db/save! (db/->path :topic (:topic/id topic)) topic))

(defn create-topic!
  "Given topic name, create and save map of topic details."
  [name]
  (let [topic {:topic/id (uuid/random)
               :topic/name name
               :topic/user-count 0}]
    (save-topic! topic)
    topic))

(defn delete-topic!
  [topic-id]
  (java.io/delete-file (db/->path :topic topic-id)))

;; event

(defn get-event
  "Return event for given event-id"
  [event-id]
  (when event-id
    (db/parse-file (db/->path :event event-id))))

(defn get-events-for-user
  "Return events that include user with given `user-id`"
  [user-id]
  (->> (db/get-entities :event)
       (filter (fn [event]
                 (contains? (:event/guest-ids event) user-id)))))

(defn save-event!
  [event]
  (db/save! (db/->path :event (:event/id event)) event))


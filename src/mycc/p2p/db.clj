(ns mycc.p2p.db
  (:require
    [mycc.common.db :as db]))

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


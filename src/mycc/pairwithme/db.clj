(ns mycc.pairwithme.db
  (:require
    [malli.core :as m]
    [mycc.common.db :as db]
    [mycc.pairwithme.schema :as schema]))

(defn get-session
  [session-id]
  (when (and session-id
             (db/entity-file-exists? :pairwithme-session session-id))
    (db/parse-file (db/->path :pairwithme-session session-id))))

(defn get-sessions-for-user
  [user-id]
  (->> (db/get-entities :pairwithme-session)
       (filter (fn [session]
                 (contains? #{(:pairwithme-session/initiator-id session)
                              (:pairwithme-session/recipient-id session)}
                            user-id)))))

(defn save-session!
  [session]
  (when-not (m/validate schema/PairwithmeSession session)
    (throw (ex-info "Invalid pairwithme session"
                    {:errors (m/explain schema/PairwithmeSession session)})))
  (db/save! (db/->path :pairwithme-session (:pairwithme-session/id session)) session))

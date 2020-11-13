(ns dojo.seed
  (:require
    [bloom.commons.uuid :as uuid]
    [dojo.model :as model]
    [dojo.db :as db]))

(defn seed! []
  (let [topics (for [topic ["react" "clojure" "reagent" "re-frame" "javascript"]]
                 {:topic/id (uuid/random)
                  :topic/name topic})
        users [{:user/id (uuid/random)
                :user/name "Alice"
                :user/email "alice@example.com"
                :user/topic-ids (set (map :topic/id (take 2 topics)))
                :user/availability (model/random-availability)}]]
    (doseq [topic topics]
      (db/save-topic! topic))
    (doseq [user users]
      (db/save-user! user))))

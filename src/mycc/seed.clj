(ns mycc.seed
  (:require
   [bloom.commons.uuid :as uuid]
   [mycc.p2p.util :as util]
   [mycc.p2p.db :as p2p.db]
   [mycc.common.db :as db]))

(defn seed! []
  (let [topics (for [topic ["react" "clojure" "reagent" "re-frame" "javascript"]]
                 {:topic/id (uuid/random)
                  :topic/name topic})
        users [{:user/id (uuid/random)
                :user/name "Alice"
                :user/email "alice@example.com"
                :user/topic-ids (set (take 2 (shuffle (map :topic/id topics))))
                :user/availability (util/random-availability)}
               {:user/id (uuid/random)
                :user/name "Bob"
                :user/email "bob@example.com"
                :user/topic-ids (set (take 2 (shuffle (map :topic/id topics))))
                :user/availability (util/random-availability)}]]
    (doseq [topic topics]
      (p2p.db/save-topic! topic))
    (doseq [user users]
      (db/save-user! user))))

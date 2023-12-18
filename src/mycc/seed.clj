(ns mycc.seed
  (:require
   [clojure.java.io :as io]
   [bloom.commons.uuid :as uuid]
   [mycc.config :refer [config]]
   [mycc.model :as model]
   [mycc.db :as db]))

(defn seed! []
  (let [topics (for [topic ["react" "clojure" "reagent" "re-frame" "javascript"]]
                 {:topic/id (uuid/random)
                  :topic/name topic})
        users [{:user/id (uuid/random)
                :user/name "Alice"
                :user/email "alice@example.com"
                :user/topic-ids (set (take 2 (shuffle (map :topic/id topics))))
                :user/availability (model/random-availability)}
               {:user/id (uuid/random)
                :user/name "Bob"
                :user/email "bob@example.com"
                :user/topic-ids (set (take 2 (shuffle (map :topic/id topics))))
                :user/availability (model/random-availability)}]]
    (doseq [topic topics]
      (db/save-topic! topic))
    (doseq [user users]
      (db/save-user! user))))

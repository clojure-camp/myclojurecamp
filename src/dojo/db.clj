(ns dojo.db
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as java.io]
    [bloom.commons.uuid :as uuid]
    [bloom.commons.thread-safe-io :as io]
    [dojo.config :refer [config]]))

(def data-path (delay (@config :data-path)))

(defn parse [f]
  (edn/read-string (io/slurp f)))

(defn ->path [entity-type entity-id]
  (str @data-path "/" (name entity-type) "/" entity-id ".edn"))

(defn get-user
  [user-id]
  (when user-id
    (parse (->path :user user-id))))

(defn get-users []
  (->> (java.io/file (str @data-path "/user"))
       file-seq
       (filter (fn [f]
                 (.isFile f)))
       (map parse)))

(defn get-topics
  []
  (let [topic-id->count (->> (get-users)
                             (map :user/topic-ids)
                             (apply concat)
                             frequencies)]
    (->> (java.io/file (str @data-path "/topic"))
         file-seq
         (filter (fn [f]
                   (.isFile f)))
         (map parse)
         (map (fn [topic]
                (assoc topic :topic/user-count
                  (or (topic-id->count (:topic/id topic))
                      0)))))))

(defn save-user! [user]
  (io/spit (->path :user (:user/id user)) user))

(defn save-topic! [topic]
  (io/spit (->path :topic (:topic/id topic)) topic))

(defn create-topic! [name]
  (let [topic {:topic/id (uuid/random)
               :topic/name name}]
    (save-topic! topic)
    topic))

(defn get-user-by-email
  [email]
  (->> (java.io/file @data-path)
       file-seq
       (filter (fn [f] (.isFile f)))
       (map parse)
       (filter (fn [u]
                 (= email (:user/email u))))
       first))

(defn create-user!
  "Create, save and return a new user"
  [email]
  (let [user {:user/id (uuid/random)
              :user/pair-next-week? false
              :user/email email
              :user/topic-ids #{}
              :user/availability {}}]
    (save-user! user)
    user))

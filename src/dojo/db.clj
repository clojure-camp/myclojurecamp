(ns dojo.db
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as java.io]
    [clojure.string :as string]
    [bloom.commons.uuid :as uuid]
    [bloom.commons.thread-safe-io :as io]
    [dojo.config :refer [config]]))

(defn parse [f]
  (edn/read-string (io/slurp f)))

(defn ->path [entity-type entity-id]
  (str (:data-path @config) "/" (name entity-type) "/" entity-id ".edn"))

(defn exists?
  [type id]
  (.exists (java.io/file (->path type id))))

(defn get-user
  [user-id]
  (when user-id
    (parse (->path :user user-id))))

(defn get-users []
  (->> (java.io/file (str (:data-path @config) "/user"))
       file-seq
       (filter (fn [f]
                 (.isFile f)))
       (map parse)))

(defn get-topic
  [topic-id]
  (when topic-id
    (parse (->path :topic topic-id))))

(defn get-topics
  []
  (let [topic-id->count (->> (get-users)
                             (map :user/topic-ids)
                             (apply concat)
                             frequencies)]
    (->> (java.io/file (str (:data-path @config) "/topic"))
         file-seq
         (filter (fn [f]
                   (.isFile f)))
         (map parse)
         (map (fn [topic]
                (assoc topic :topic/user-count
                  (or (topic-id->count (:topic/id topic))
                      0)))))))

(defn topic-name-exists?
  [name]
  (->> (get-topics)
       (some (fn [topic]
               (= (:topic/name topic) name)))))

(defn save-user! [user]
  (io/spit (->path :user (:user/id user)) user))

(defn save-topic! [topic]
  (io/spit (->path :topic (:topic/id topic)) topic))

(defn create-topic! [name]
  (let [topic {:topic/id (uuid/random)
               :topic/name name}]
    (save-topic! topic)
    topic))

(defn normalize-email [email]
  (-> email
     (string/replace #"\s" "")
     (string/lower-case)))

#_(normalize-email "\nfOO@example .com")

(defn extract-name-from-email [email]
  (-> email
      normalize-email
      (string/split #"@" 2)
      first))

#_(extract-name-from-email "alice@example.com")

(defn get-user-by-email
  [email]
  (->> (java.io/file (:data-path @config))
       file-seq
       (filter (fn [f] (.isFile f)))
       (map parse)
       (filter (fn [u]
                 (= (normalize-email email)
                    (:user/email u))))
       first))

(defn create-user!
  "Create, save and return a new user"
  [email]
  (let [user {:user/id (uuid/random)
              :user/pair-next-week? false
              :user/email (normalize-email email)
              :user/name (extract-name-from-email email)
              :user/max-pair-per-day 0
              :user/max-pair-per-week 0
              :user/topic-ids #{}
              :user/availability {}}]
    (save-user! user)
    user))

(ns dojo.db
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as java.io]
    [clojure.string :as string]
    [bloom.commons.uuid :as uuid]
    [bloom.commons.thread-safe-io :as io]
    [dojo.config :refer [config]]))

;; three main types of entities:
;;    user, topic, event
;; which are maps
;; stored as edn files in folder path defined in config.edn

;; note: not thread-safe

;; generic

(defn- parse-file
  [f]
  (edn/read-string (io/slurp f)))

(defn- ->path
  "File path for given entity-type and (optional) entity-id "
  ([entity-type]
   (str (:data-path @config) "/" (name entity-type)))
  ([entity-type entity-id]
   (str (:data-path @config) "/" (name entity-type) "/" entity-id ".edn")))

(defn entity-file-exists?
  "Return if an entity file exists?"
  [type id]
  (.exists (java.io/file (->path type id))))

(defn- get-entities
  [entity-type]
  (->> (java.io/file (->path entity-type))
       file-seq
       (filter (fn [f]
                 (.isFile f)))
       (filter (fn [f]
                 (string/ends-with? (.getName f) "edn")))
       (map parse-file)))

(defn- save!
  "Write content to file-path. (If parent directory doesn't exist, it will be created.)"
  [file-path content]
  (.mkdirs (.getParentFile (java.io/file file-path)))
  (io/spit file-path content))

;; user

(defn get-user
  "Return user for given user-id."
  [user-id]
  (when user-id
    (parse-file (->path :user user-id))))

(defn get-users
  "Returns all users."
  []
  (get-entities :user))

(defn save-user! [user]
  (save! (->path :user (:user/id user)) user))

(defn- normalize-email
  [email]
  (-> email
      (string/replace #"\s" "")
      (string/lower-case)))

#_(normalize-email "\nfOO@example .com")

(defn- extract-name-from-email
  "Return the local part of `email` (ie. the part before the @)."
  [email]
  (-> email
      normalize-email
      (string/split #"@" 2)
      first))

#_(extract-name-from-email "alice@example.com")

(defn get-user-by-email
  "Returns user with given email"
  [email]
  (->> (get-users)
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
              :user/max-pair-per-day 1
              :user/max-pair-per-week 1
              :user/topic-ids #{}
              :user/availability {}
              :user/email-validated? false
              :user/subscribed? true}]
    (save-user! user)
    user))

;; topic

(defn get-topic
  "Return topic matching topic-id (without :topic/user-count)"
  [topic-id]
  (when topic-id
    (parse-file (->path :topic topic-id))))

(defn- user-topic-frequencies
  "Returns a map of topic-id->user-count"
  []
  (->> (get-users)
       (mapcat :user/topic-ids)
       frequencies))

(defn- get-topics-raw
  "Return topics (without :topic/user-count)"
  []
  (get-entities :topic))

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
  (save! (->path :topic (:topic/id topic)) topic))

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
  (java.io/delete-file (->path :topic topic-id)))

;; event

(defn get-event
  "Return event for given event-id"
  [event-id]
  (when event-id
    (parse-file (->path :event event-id))))

(defn get-events-for-user
  "Return events that include user with given `user-id`"
  [user-id]
  (->> (get-entities :event)
       (filter (fn [event]
                 (contains? (:event/guest-ids event) user-id)))))

(defn save-event!
  [event]
  (save! (->path :event (:event/id event)) event))


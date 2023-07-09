(ns dojo.db
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as java.io]
    [clojure.string :as string]
    [bloom.commons.uuid :as uuid]
    [bloom.commons.thread-safe-io :as io]
    [dojo.config :refer [config]]))

;; The data for user, topic and event details are stored on disk in path defined in config.edn

(defn parse
  [f]
  (edn/read-string (io/slurp f)))

;; in the doc string "config as context" (added by Paula) indicates config is not an argument. note: this fn is not referentially transparent
(defn ->path
  "Given entity-type and entity-id, create path to entity (using config as context) and return path as string"
  [entity-type entity-id]
  (str (:data-path @config) "/" (name entity-type) "/" entity-id ".edn"))

(defn entity-file-exists?
  "Given entity-type and entity-id, check if entity file exists"
  [type id]
  (.exists (java.io/file (->path type id))))

(defn get-user
  "Given user-id, return map of user details"
  [user-id]
  (when user-id
    (parse (->path :user user-id))))

(defn get-users
  "Returns a sequence of maps of user details using config as context."
  []
  (->> (java.io/file (str (:data-path @config) "/user"))
       file-seq
       (filter (fn [f]
                 (.isFile f)))
       (filter (fn [f]
                 (string/ends-with? (.getName f) "edn")))
       (map parse)))

(defn get-topic
  "Given topic-id, return map of topic details"
  [topic-id]
  (when topic-id
    (parse (->path :topic topic-id))))

(defn user-topic-frequencies
  "Returns a map of topic-id->user-count"
  []
  (->> (get-users)
       (mapcat :user/topic-ids)
       frequencies))

(defn get-topics-raw
  "Returns a sequence of maps of topic details"
  []
  (->> (java.io/file (str (:data-path @config) "/topic"))
       file-seq
       (filter (fn [f]
                 (.isFile f)))
       (filter (fn [f]
                 (string/ends-with? (.getName f) "edn")))
       (map parse)))

(defn get-topics
  "Read all topics from files and add user count. Returns a sequence of maps"
  []
  (let [topic-id->count (user-topic-frequencies)]
    (->> (get-topics-raw)
         (map (fn [topic]
                (assoc topic :topic/user-count
                  (or (topic-id->count (:topic/id topic))
                      0)))))))

(defn topic-name-exists?
  "Returns true if topic name is found in map of topic details"
  [name]
  (->> (get-topics)
       (some (fn [topic]
               (= (:topic/name topic) name)))))

(defn save!
  "Write content to file-path. If parent directory doesn't exist, create it."
  [file-path content]
  (.mkdirs (.getParentFile (java.io/file file-path)))
  (io/spit file-path content))

(defn save-user! [user]
  (save! (->path :user (:user/id user)) user))

(defn save-topic! [topic]
  (save! (->path :topic (:topic/id topic)) topic))

(defn save-event! [event]
  (save! (->path :event (:event/id event)) event))

(defn get-event
  "Given event-id, return map of event details"
  [event-id]
  (when event-id
    (parse (->path :event event-id))))

(defn get-events-for-user
  "Return only events (as seq of maps) that include user with `user-id`"
  [user-id]
  (->> (java.io/file (str (:data-path @config) "/event"))
       file-seq
       (filter (fn [f]
                 (.isFile f)))
       (map parse)
       (filter (fn [event]
                 (contains? (:event/guest-ids event) user-id)))))

(defn create-topic!
  "Given topic name, create and save map of topic details."
  [name]
  (let [topic {:topic/id (uuid/random)
               :topic/name name}]
    (save-topic! topic)
    topic))

(defn delete-topic! [topic-id]
  (java.io/delete-file (->path :topic topic-id)))

(defn normalize-email
  "Given email, return email with whitespace characters removed and converted to lowercase."
  [email]
  (-> email
     (string/replace #"\s" "")
     (string/lower-case)))

#_(normalize-email "\nfOO@example .com")

(defn extract-name-from-email
  ;"Given email, return local part of email."
  "Return the local part of `email`."
  [email]
  (-> email
      normalize-email
      (string/split #"@" 2)
      first))

#_(extract-name-from-email "alice@example.com")

(defn get-user-by-email
  "Given email, return map of user details which contain input email."
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
              :user/max-pair-per-day 1
              :user/max-pair-per-week 1
              :user/topic-ids #{}
              :user/availability {}
              :user/email-validated? false
              :user/subscribed? true}]
    (save-user! user)
    user))

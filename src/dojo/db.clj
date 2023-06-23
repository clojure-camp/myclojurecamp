(ns dojo.db
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as java.io]
    [clojure.string :as string]
    [bloom.commons.uuid :as uuid]
    [bloom.commons.thread-safe-io :as io]
    [dojo.config :refer [config]]))

(defn parse
  "Given filename as string, return contents"
  [f]
  (edn/read-string (io/slurp f)))

;; in the doc string "config as context" (added by Paula) indicates config is not an argument. note: this fn is not referentially transparent
(defn ->path
  "Given entity-type and entity-id as strings, create path to entity (using config as context) and return path as string"
  [entity-type entity-id]
  (str (:data-path @config) "/" (name entity-type) "/" entity-id ".edn"))

(defn entity-file-exists?
  "Given entity-type and entity-id as strings, check if entity file exists"
  [type id]
  (.exists (java.io/file (->path type id))))

(defn get-user
  "Given user-id as string, return map of user details"
  [user-id]
  (when user-id
    (parse (->path :user user-id))))

(defn get-users
  "Returns a collection maps of user details"
  []
  (->> (java.io/file (str (:data-path @config) "/user"))
       file-seq
       (filter (fn [f]
                 (.isFile f)))
       (map parse)))


(defn get-topic
  "Given topic-id as string, return map of topic details"
  [topic-id]
  (when topic-id
    (parse (->path :topic topic-id))))


(defn user-topic-frequencies
  "Returns a map of topic-id->user-count"
  []
  (->> (get-users)
      (map :user/topic-ids)
      (apply concat)
      frequencies))

(defn get-topics
  "Returns a collection of maps of topic details"
  []
  (->> (java.io/file (str (:data-path @config) "/topic"))
       file-seq
       (filter (fn [f]
                 (.isFile f)))
       (map parse)))

(defn update-topic-interests
  "Add user count for each topic to collection of maps of topic details"
  []
  (let [topics (get-topics)
        user-interest (user-topic-frequencies)]
    (->> topics
         (map (fn [topic]
                (assoc topic :topic/user-count
                             (or (user-interest (:topic/id topic)
                                                0))))))))

; x->y means map with key x and value y (e.g. topic-id->count)
;; deconstructed this function into 3 different ones above
#_(defn get-topics
    "Read all topics from files and add user count. Returns a collection of maps"
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
  "Returns true if topic name is found in map of topic details"
  [name]
  (->> (get-topics)
       (some (fn [topic]
               (= (:topic/name topic) name)))))

(defn save!
  "Create parent directory if it doesn't exist.
  Then, spit content to file-path"
  [file-path content]
  (.mkdirs (.getParentFile (java.io/file file-path)))
  (io/spit file-path content))

(defn save-user! [user]
  "Given map of user details, save map to path"
  (save! (->path :user (:user/id user)) user))

(defn save-topic! [topic]
  "Given map of topic details, save map to path"
  (save! (->path :topic (:topic/id topic)) topic))

(defn save-event! [event]
  "Given map of event details, save map to path"
  (save! (->path :event (:event/id event)) event))

(defn get-event
  "Given event-id as string, return map of event details"
  [event-id]
  (when event-id
    (parse (->path :event event-id))))

(defn get-events-for-user
  "Given user-id as string, return collection of map of event details where user is included"
  [user-id]
  (->> (java.io/file (str (:data-path @config) "/event"))
       file-seq
       (filter (fn [f]
                 (.isFile f)))
       (map parse)
       (filter (fn [event]
                 (contains? (:event/guest-ids event) user-id)))))

(defn create-topic! [name]
  (let [topic {:topic/id (uuid/random)
               :topic/name name}]
    (save-topic! topic)
    topic))

(defn delete-topic! [topic-id]
  (java.io/delete-file (->path :topic topic-id)))

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
              :user/max-pair-per-day 1
              :user/max-pair-per-week 1
              :user/topic-ids #{}
              :user/availability {}
              :user/email-validated? false
              :user/subscribed? true}]
    (save-user! user)
    user))

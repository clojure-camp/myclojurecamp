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


(defn ->path
  [entity-type entity-id]
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

(defn save! [file-path content]
  ;; make sure parent directory exists or spit will error
  (.mkdirs (.getParentFile (java.io/file file-path)))
  (io/spit file-path content))

(defn save-user! [user]
  (save! (->path :user (:user/id user)) user))

(defn save-topic! [topic]
  (save! (->path :topic (:topic/id topic)) topic))

(defn save-event! [event]
  (save! (->path :event (:event/id event)) event))

(defn get-event
  [event-id]
  (when event-id
    (parse (->path :event event-id))))

(defn get-events-for-user [user-id]
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

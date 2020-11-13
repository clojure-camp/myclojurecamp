(ns dojo.db
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as java.io]
    [bloom.commons.uuid :as uuid]
    [bloom.commons.thread-safe-io :as io]
    [dojo.config :refer [config]]))

(def data-path (delay (config :data-path)))

(defn parse [f]
  (edn/read-string (io/slurp f)))

(defn ->path [entity-type entity-id]
  (str @data-path "/" (name entity-type) "/" entity-id ".edn"))

(defn get-user
  [user-id]
  (when user-id
    (parse (->path :user user-id))))

(defn get-topics
  []
  (->> (java.io/file (str @data-path "/topic"))
       file-seq
       (filter (fn [f]
                 (.isFile f)))
       (map parse)))

(defn save-user! [user]
  (io/spit (->path :user (:user/id user)) user))

(defn save-topic! [topic]
  (io/spit (->path :topic (:topic/id topic)) topic))

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
              :user/email email
              :user/topic-ids #{}
              :user/availability {}}]
    (save-user! user)
    user))

(ns mycc.common.db
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as java.io]
    [clojure.string :as string]
    [bloom.commons.uuid :as uuid]
    [bloom.commons.thread-safe-io :as io]
    ;; TODO using directly b/c of circular dependency
    ;; parts of this namespace should be moved under modulo anyway
    [modulo.config :as mod]))

;; entities, which are maps
;; stored as edn files in folder path defined in config.edn

;; generic

(defn parse-file
  [f]
  (edn/read-string (io/slurp f)))

(defn ->path
  "File path for given entity-type and (optional) entity-id "
  ([entity-type]
   (str (mod/config :data-path) "/" (name entity-type)))
  ([entity-type entity-id]
   (str (mod/config :data-path) "/" (name entity-type) "/" entity-id ".edn")))

(defn entity-file-exists?
  "Return if an entity file exists?"
  [type id]
  (.exists (java.io/file (->path type id))))

(defn get-entities
  [entity-type]
  (->> (java.io/file (->path entity-type))
       file-seq
       (filter (fn [f]
                 (.isFile f)))
       (filter (fn [f]
                 (string/ends-with? (.getName f) "edn")))
       (map parse-file)))

(defn save!
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
              :user/role :role/student
              :user/max-pair-per-day 1
              :user/max-pair-per-week 1
              :user/topic-ids #{}
              :user/availability {}
              :user/email-validated? false
              :user/subscribed? true}]
    (save-user! user)
    user))

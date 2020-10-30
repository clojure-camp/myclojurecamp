(ns dojo.db
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as java.io]
    [bloom.commons.uuid :as uuid]
    [bloom.commons.thread-safe-io :as io]
    [dojo.config :refer [config]]))

(def user-data-path (config :data-path))

(defn parse [f]
  (edn/read-string (io/slurp f)))

(defn ->path [user-id]
  (str user-data-path "/" user-id ".edn"))

(defn get-user
  [user-id]
  (when user-id
    (parse (->path user-id))))

(defn save-user! [user]
  (io/spit (->path (:user/id user)) user))

(defn get-user-by-email
  [email]
  (->> (java.io/file user-data-path)
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
              :user/availability {}}]
    (save-user! user)
    user))

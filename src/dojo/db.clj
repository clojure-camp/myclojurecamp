(ns dojo.db
  (:require
    [clojure.edn :as edn]
    [bloom.commons.thread-safe-io :as io]))

(def user-data-path "./external/users")

(defn ->path [user-id]
  (str user-data-path "/" user-id ".edn"))

(defn get-user
  [user-id]
  (edn/read-string (io/slurp (->path user-id))))

(defn save-user! [user]
  (io/spit (->path (:user/id user)) user))

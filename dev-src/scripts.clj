(ns scripts
  (:import
    [java.nio.file Files FileSystems LinkOption]))

;; add :role/student to all users
#_(->> (mycc.common.db/get-users)
       (map (fn [u]
              (assoc u :user/role :role/student)))
       (map mycc.common.db/save-user!)
       doall)

(defn file-attribute
  "Return the value of the specified `attribute` of the file at `file-path`
  in the current default file system. The argument `attribute` may be passed
  as a keyword or a string, but must be an attribute name understood be
  `java.nio.file.Files`."
  [file-path attribute]
  (Files/getAttribute
    (.getPath
      (FileSystems/getDefault)
      (str file-path)
      (into-array java.lang.String []))
    (name attribute)
    (into-array LinkOption [])))

;; add :user/created-at to all users, based on system file creation time
#_(->> (mycc.common.db/get-users)
       (remove :user/created-at)
       (map (fn [{:user/keys [id] :as u}]
              (assoc u :user/created-at
                     (java.util.Date.
                      (.toMillis (file-attribute
                                  (db/->path :user id)
                                  :creationTime))))))
       (map mycc.common.db/save-user!)
       doall)

;; add :user/max-pair-same-user
#_(->> (mycc.common.db/get-users)
       (map (fn [u]
              (assoc u :user/max-pair-same-user 2)))
       (map mycc.common.db/save-user!)
       doall)

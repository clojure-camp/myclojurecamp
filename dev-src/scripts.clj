(ns scripts)

;; add :role/student to all users
#_(->> (mycc.common.db/get-users)
       (map (fn [u]
              (assoc u :user/role :role/student)))
       (map mycc.common.db/save-user!)
       doall)

;; add :user/created-at to all users, based on system file creation time
#_(->> (mycc.common.db/get-users)
       (remove :user/created-at)
       (map (fn [{:user/keys [id] :as u}]
              (assoc u :user/created-at
                (->> (mycc.common.db/->path :user id)
                     ;; using ext4, have creation date
                     ;; if this didn't work, could fall back to modified date (%Y)
                     (clojure.java.shell/sh "stat" "-c" "%W" )
                     :out
                     clojure.string/trim
                     (memfn Long.)
                     (* 1000)
                     (memfn java.util.Date.)))))
       (map mycc.common.db/save-user!)
       doall)

;; add :user/max-pair-same-user
#_(->> (mycc.common.db/get-users)
       (map (fn [u]
              (assoc u :user/max-pair-same-user 2)))
       (map mycc.common.db/save-user!)
       doall)

;; add empty :user/pair-opt-in-history
#_(->> (mycc.common.db/get-users)
       (map (fn [u]
              (assoc u :user/pair-opt-in-history #{})))
       (map mycc.common.db/save-user!)
       doall)

#_(->> (mycc.common.db/get-users)
       (filter :user/pair-next-week?)
       (map (fn [u]
              (update u :user/pair-opt-in-history conj
                      (mycc.common.date/upcoming-monday))))
       (map mycc.common.db/save-user!)
       doall)

;; validate users
#_(->> (mycc.common.db/get-users)
       (map (fn [u]
              (malli.error/humanize (malli.core/explain mycc.base.schema/User u))))
       first)

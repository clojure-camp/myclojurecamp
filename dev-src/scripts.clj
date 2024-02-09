(ns scripts)

;; add :role/student to all users
#_(->> (mycc.common.db/get-users)
       (map (fn [u]
              (assoc u :user/role :role/student)))
       (map mycc.common.db/save-user!)
       doall)

;; add :user/max-pair-same-user
#_(->> (mycc.common.db/get-users)
       (map (fn [u]
              (assoc u :user/max-pair-same-user 2)))
       (map mycc.common.db/save-user!)
       doall)

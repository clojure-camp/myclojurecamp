(ns scripts)

;; add :role/student to all users
#_(->> (mycc.common.db/get-users)
       (map (fn [u]
              (assoc u :user/role :role/student)))
       (map mycc.common.db/save-user!)
       doall)

(ns scripts)

;; add :role/student to all users
#_(->> (mycc.db/get-users)
       (map (fn [u]
              (assoc u :user/role :role/student)))
       (map mycc.db/save-user!)
       doall)

(ns scripts)

;; add :role/student to all users
#_(->> (dojo.db/get-users)
       (map (fn [u]
              (assoc u :user/role :role/student)))
       (map dojo.db/save-user!)
       doall)

(ns migrations)

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
                     (clojure.java.shell/sh ")stat" "-c" "%W" )
                     :out
                     clojure.string/trim
                     (memfn Long.)
                     (* 1000)
                     (memfn java.util.Date.)))))
       (map mycc.common.db/save-user!)
       doall)

#_(->> (mycc.common.db/get-users)
       (map (fn [u]
              (some-> u
                      (assoc :user/pair-next-week? true)
                      (update :user/pair-opt-in-history conj
                              (mycc.common.date/next-monday))
                      mycc.common.db/save-user!)))
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
                      (mycc.common.date/next-monday))))
       (map mycc.common.db/save-user!)
       doall)

#_(->> (mycc.common.db/get-users)
       (map (fn [u]
              (update u :user/user-pair-deny-list
                      (fn [x]
                        (if x
                          x
                          #{})))))
       (map mycc.common.db/save-user!)
       doall)

;; add empty :user/primary-languages & :user/secondary-languages
#_(->> (mycc.common.db/get-users)
       (map (fn [u]
              (-> u
                  (update :user/primary-languages (fn [l]
                                                    (or l #{})))
                  (update :user/secondary-languages (fn [l]
                                                      (or l #{}))))))
       (map mycc.common.db/save-user!)
       doall)

;; migrate from topic-ids to topics
;; #{1 2} => {1 :level/beginner 2 :level/expert}
#_(->> (mycc.common.db/get-users)
       (map (fn [{:user/keys [topic-ids role] :as u}]
              (let [role (or role :role/student)
                    level (if (= :role/student role)
                            :level/beginner
                            :level/expert)]
                (-> u
                    (assoc :user/topics (zipmap topic-ids (repeat level)))
                    (dissoc :user/topic-ids)))))
       (map mycc.common.db/save-user!)
       doall)

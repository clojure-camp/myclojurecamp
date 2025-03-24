(ns scripts)

;; time zone checks

;; run Intl.supportedValuesOf("timeZone") in browser and save to tz.edn

#_(clojure.set/difference (set (read-string (slurp "tz.edn")))
                          (set (java.time.ZoneId/getAvailableZoneIds)))

#_(keep (fn [x]
          (if (try
                (java.time.ZoneId/of x)
                (catch Exception _
                  false))
            nil
            x))
        (set (read-string (slurp "tz.edn"))))

;; validate users
#_(->> (mycc.common.db/get-users)
       (filter (fn [u]
                   (clojure.string/includes? (:user/email u)
                                             "jf")))
       (map (fn [u]
              (if-let [e (malli.error/humanize (malli.core/explain mycc.base.schema/User u))]
                [(:user/email u) e]
                )))
       first)

;; see what events will be created
#_(let [date (tick.core/date "2024-04-01")]
    (mycc.p2p.match-email-job/generate-schedule
      (filter (fn [u]
                (contains? (:user/pair-opt-in-history u) date))
              (mycc.common.db/get-users))
      date))

;; availability in time zone for user
#_(let [user-id #uuid "79af1d15-97ae-45c8-98ca-f95ec9118a19"
        to-time-zone "America/Toronto"
        monday (tick.core/date "2024-04-01")
        user (mycc.common.db/get-user user-id)]
    (->> (:user/availability user)
         (filter (fn [[_ available?]] available?))
         (map (fn [[day-and-hour _]]
                (mycc.common.date/convert-time
                  day-and-hour
                  (:user/time-zone user)
                  monday)))
         (map (fn [t]
                (tick.core/in t (tick.core/zone to-time-zone))))))

;; resave all users
#_(->> (mycc.common.db/get-users)
      (map mycc.common.db/save-user!)
      doall)

;; resave all topics
#_(->> (mycc.p2p.db/get-topics)
       (map mycc.p2p.db/save-topic!)
       doall)

(defn merge-topics!
  [keep-topic-title remove-topic-title]
  (let [topics (mycc.common.db/get-entities :topic)
        keep-topic (->> topics
                        (filter (fn [t]
                                  (= (:topic/name t) keep-topic-title)))
                        first)
        remove-topic (->> topics
                         (filter (fn [t]
                                   (= (:topic/name t) remove-topic-title)))
                         first)
        _ (assert keep-topic)
        _ (assert remove-topic)
        users (mycc.common.db/get-users)]

    ;; update users with remove-topic to have keep-topic
    (->> users
         (filter (fn [user]
                   (contains? (:user/topics user) (:topic/id remove-topic))))
         (map (fn [user]
                (let [remove-topic-level (get-in user [:user/topics (:topic/id remove-topic)])]
                  (-> user
                      (update :user/topics dissoc (:topic/id remove-topic))
                      (update-in [:user/topics (:topic/id keep-topic)]
                                 (fn [keep-topic-level]
                                   ;; keep the highest
                                   (max-key {nil 0
                                             :level/beginner 1
                                             :level/intermediate 2
                                             :level/expert 3}
                                            keep-topic-level
                                            remove-topic-level)))))))
         (map mycc.common.db/save-user!)
         doall)

    ;; delete remove-topic
    (clojure.java.io/delete-file (mycc.common.db/->path :topic (:topic/id remove-topic)))))

#_(merge-topics! "datomic" "Datomic")

;; topics by alpha and # of uses
#_(let [topic-freqs (->> (mycc.common.db/get-users)
                         (mapcat (fn [user]
                                   (->> (:user/topics user)
                                        (filter val)
                                        (map key))))
                         frequencies)]
    (->> (mycc.common.db/get-entities :topic)
         (sort-by (comp clojure.string/lower-case :topic/name))
         (map (juxt :topic/name (fn [t] (topic-freqs (:topic/id t)))))))


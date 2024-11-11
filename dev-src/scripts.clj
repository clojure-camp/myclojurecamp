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

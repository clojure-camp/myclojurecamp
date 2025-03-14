(ns mycc.p2p.availability
  (:require
   [mycc.common.db :as db]
   [mycc.common.date :as date])
  (:import
   [java.time LocalDate]))

(defn time-shift [reference-local-date from-time-zone to-time-zone [day-of-week hour-of-day]]
  (let [new-date-time (-> (date/convert-time [day-of-week hour-of-day] from-time-zone reference-local-date)
                          (date/shift-zone to-time-zone))]
    [(date/java-day-of-week-> (.getDayOfWeek new-date-time))
     (.getHour new-date-time)]))

#_(time-shift (date/next-monday) "America/Toronto" "Europe/Dublin" [:monday 9])

(defn global-availability [reference-user-id]
  ;; returns {[:monday 8] 0.2 ...} based on all active user's availabilities
  ;; (as a percentage of max users availability at a given time)
  (let [reference-user (db/get-user reference-user-id)
        next-monday (date/next-monday)
        four-months-ago (.minus (LocalDate/now)
                                (java.time.Period/ofMonths 4))
        freqs (->> (db/get-users)
                   (filter :user/subscribed?)
                   (filter (fn [user]
                            (seq (:user/pair-opt-in-history user))))
                   (remove (fn [user]
                             (= (:user/id user)
                                reference-user-id)))
                   ;; only include users who have participated in the last 4 months
                   (filter (fn [user]
                             (.isBefore
                              four-months-ago
                              (last (sort (:user/pair-opt-in-history user))))))
                   (mapcat (fn [user]
                             (->> (:user/availability user)
                                  (filter (fn [[_ v]] v))
                                  (map first)
                                  ;; [[:monday 10] ...]
                                  (map (partial time-shift
                                                next-monday
                                                (:user/time-zone user)
                                                (:user/time-zone reference-user))))))
                   frequencies)
        max-count (apply max (vals freqs))]
    (-> freqs
        ;; normalize linearly such that highest value is 1.0
        (update-vals #(double (/ % max-count))))))

#_(global-availability (->> (db/get-users)
                            first
                            :user/id))



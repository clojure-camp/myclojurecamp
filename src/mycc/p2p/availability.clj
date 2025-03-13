(ns mycc.p2p.availability
  (:require [mycc.common.db :as db]
            [mycc.common.date :as date]))

(defn time-shift [reference-local-date from-time-zone to-time-zone [day-of-week hour-of-day]]
  (let [new-date-time (-> (date/convert-time [day-of-week hour-of-day] from-time-zone reference-local-date)
                          (date/shift-zone to-time-zone))]
    [(date/java-day-of-week-> (.getDayOfWeek new-date-time))
     (.getHour new-date-time)]))

#_(time-shift (date/next-monday) "America/Toronto" "Europe/Dublin" [:monday 9])

(defn global-availability [reference-user-id]
  ;; Returns {[:monday 8] 0.2 ...}
  (let [reference-user (db/get-user reference-user-id)
        next-monday (date/next-monday)
        users (->> (db/get-users)
                   (filter :user/subscribed?)
                   (remove (fn [user]
                             (= (:user/id user)
                                reference-user-id))))]
    (-> users
        (->> (mapcat (fn [user]
                       (->> (:user/availability user)
                            (filter (fn [[_ v]] v))
                            (map first)
                            ;; [[:monday 10] ...]
                            (map (partial time-shift
                                          next-monday
                                          (:user/time-zone user)
                                          (:user/time-zone reference-user)))))))
        frequencies
        (update-vals #(double (/ % (count users)))))))

#_(global-availability (->> (db/get-users)
                            first
                            :user/id))

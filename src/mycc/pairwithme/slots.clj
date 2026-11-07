(ns mycc.pairwithme.slots
  (:require
    [mycc.common.date :as date])
  (:import
    (java.time ZoneId ZonedDateTime)
    (java.time.format DateTimeFormatter)
    (java.time.temporal ChronoUnit)
    (java.util Date)))

(def lead-time-hours 12)
(def window-days 7)

(defn ->zone-id
  [user]
  (ZoneId/of (or (:user/time-zone user) "UTC")))

(defn ->zoned
  [^Date inst zone-id]
  (ZonedDateTime/ofInstant (.toInstant inst) zone-id))

(defn availability-at
  [user ^Date inst]
  (let [zoned (->zoned inst (->zone-id user))]
    (get (:user/availability user)
         [(date/java-day-of-week-> (.getDayOfWeek zoned))
          (.getHour zoned)])))

(defn listed?
  [user]
  (boolean
    (and (:user/pairwithme-show-profile? user)
         (some some? (vals (:user/availability user))))))

(defn candidate-slots
  ;; hourly start times, aligned to whole hours in zone-id
  [^Date now zone-id]
  (let [earliest (-> (->zoned now zone-id)
                     (.plusHours lead-time-hours))
        truncated (.truncatedTo earliest ChronoUnit/HOURS)
        first-slot (if (.isEqual truncated earliest)
                     truncated
                     (.plusHours truncated 1))
        latest (-> (->zoned now zone-id)
                   (.plusDays window-days))]
    (->> (iterate (fn [slot] (.plusHours slot 1)) first-slot)
         (take-while (fn [slot] (.isBefore slot latest)))
         (map (fn [slot] (Date/from (.toInstant slot)))))))

#_(take 3 (candidate-slots (Date.) (ZoneId/of "America/Toronto")))

(defn schedule-grid
  [{:keys [now viewer other]}]
  (let [zone-id (->zone-id viewer)]
    (for [slot (candidate-slots now zone-id)
          :let [zoned (->zoned slot zone-id)]]
      {:slot slot
       :viewer-value (availability-at viewer slot)
       :other-value (availability-at other slot)
       :local-date (str (.toLocalDate zoned))
       :local-day-of-week-label (.format zoned (DateTimeFormatter/ofPattern "EEE"))
       :local-date-label (.format zoned (DateTimeFormatter/ofPattern "MMM d"))
       :local-hour (.getHour zoned)})))

(defn overlap-count
  [{:keys [now viewer other]}]
  (->> (candidate-slots now (->zone-id viewer))
       (filter (fn [slot]
                 (and (availability-at viewer slot)
                      (availability-at other slot))))
       count))

(defn valid-slot?
  [{:keys [now proposer recipient slot]}]
  (boolean
    (and (some (fn [candidate] (= candidate slot))
               (candidate-slots now (->zone-id proposer)))
         (availability-at recipient slot))))

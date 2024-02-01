(ns pairing-scheduler.core
  (:require
   [clojure.math.combinatorics :as combo]
   [clojure.set :as set])
  (:import
   (java.time Period DayOfWeek ZonedDateTime ZoneId LocalTime LocalDate)))

(defn overlapping-daytimes
  [guest-ids {:keys [availabilities]}]
  (->> guest-ids
       (map availabilities)
       (map (fn [availabilities]
              (set (map first availabilities))))
       (apply set/intersection)))

#_(overlapping-daytimes
    ["alice" "bob"]
    {:availabilities {"alice" #{[#inst "2021-01-01T09" :available]
                                [#inst "2021-01-02T10" :preferred]}
                      "bob"   #{[#inst "2021-01-01T09" :available]
                                [#inst "2021-01-02T11" :preferred]}}})

(defn random-event
  [guest-ids context]
  {:guest-ids (set guest-ids)
   :at (let [possible-times (overlapping-daytimes guest-ids context)]
        (when (seq possible-times)
          (rand-nth (vec possible-times))))})

(defn remove-from-vec
  [pos coll]
  (vec (concat (subvec coll 0 pos) (subvec coll (inc pos)))))

(defn ->zoned-date-time
  "inst should be a java.util.Date ex. #inst \"2021-01-01T09\"
   timezone should be a string, ex. \"America/Toronto\""
  [inst timezone]
  (ZonedDateTime/ofInstant (.toInstant inst) (ZoneId/of timezone)))

#_(let [at #inst "2021-01-01T09"
        tz "America/Toronto"
        day-of-week DayOfWeek/WEDNESDAY]
    (->zoned-date-time at tz))

(defn days-over-max
  [guest-id {:keys [timezones max-events-per-day schedule]}]
  (let [guest-events (->> schedule
                          (filter (fn [event]
                                    (contains? (event :guest-ids) guest-id))))]
   (if (and max-events-per-day (max-events-per-day guest-id))
     (->> [DayOfWeek/MONDAY
           DayOfWeek/TUESDAY
           DayOfWeek/WEDNESDAY
           DayOfWeek/THURSDAY
           DayOfWeek/FRIDAY
           DayOfWeek/SATURDAY
           DayOfWeek/SUNDAY]
          (map (fn [target-day-of-week]
                 (< (max-events-per-day guest-id)
                    (->> guest-events
                         (filter (fn [event]
                                   (= target-day-of-week
                                      (.getDayOfWeek (->zoned-date-time (:at event) (timezones guest-id))))))
                         count))))
          (filter true?)
          (count))
     0)))

#_(days-over-max "alice" {:timezones {"alice" "America/Toronto"}
                          :max-events-per-day {"alice" 1}
                          :schedule [{:at #inst "2021-01-01T09"
                                      :guest-ids #{"alice" "bob"}}
                                     {:at #inst "2021-01-01T10"
                                      :guest-ids #{"alice" "bob"}}]})


(defn individual-score-meta
  [guest-id {:keys [schedule availabilities timezones max-events-per-day max-events-per-week topics roles roles-to-pair-with] :as context}]
  (let [guest-events (->> schedule
                          (filter (fn [event]
                                    (contains? (event :guest-ids) guest-id))))
        guest-open-times  (and availabilities
                            (->> (availabilities guest-id)
                                 (map first)
                                 set))]
    (concat
      ;; above max for day
      (let [day-count (days-over-max guest-id context)]
        (when (< 0 day-count)
          [{:factor/id :factor.id/over-max-per-day
            :factor/score (* 50 day-count)
            :factor/meta {:days-over-max day-count}}]))

      ;; above max for week
      (when (and max-events-per-week
              (max-events-per-week guest-id)
              (< (max-events-per-week guest-id)
                 (count guest-events)))
        [{:factor/id :factor.id/over-max-per-week
          :factor/score 100
          :factor/meta {:max (max-events-per-week guest-id)
                        :count (count guest-events)}}])

      ;; per event
      (->> guest-events
           (map (fn [event]
                  ;; using negatives for ok events, to promote more events rather than fewer
                  ;; b/c otherwise, an empty schedule would always be a perfect schedule
                  (cond
                    ;; double-scheduled
                    (< 1 (->> guest-events
                              (map :at)
                              (filter (partial = (event :at)))
                              count))
                    {:factor/id :factor.id/double-scheduled
                     :factor/score 200}
                    ;; outside of any available times
                    (and
                      availabilities
                      (not (contains? guest-open-times (event :at))))
                    {:factor/id :factor.id/outside-of-available-times
                     :factor/score 100}
                    ;; if it's not with someone with matching topics
                    (and
                      topics ;; ignore this criterion if no topics passed in
                      (->> (event :guest-ids)
                           (map topics)
                           (apply set/intersection)
                           empty?))
                    {:factor/id :factor.id/without-matching-topics
                     :factor/score 99}
                    ;; matched with no acceptable role
                    (and
                      roles
                      roles-to-pair-with
                      (:acceptable (roles-to-pair-with guest-id))
                      (let [my-role-prefs (:acceptable (roles-to-pair-with guest-id))
                            others-roles (-> (event :guest-ids)
                                             (disj guest-id)
                                             first
                                             roles)]
                        (empty? (set/intersection my-role-prefs others-roles))))
                    {:factor/id :factor.id/without-matching-acceptable-role
                     :factor/score 95}
                    ;; matched with no preferred role
                    (and
                      roles
                      roles-to-pair-with
                      (:preferred (roles-to-pair-with guest-id))
                      (let [my-role-prefs (:preferred (roles-to-pair-with guest-id))
                            others-roles (-> (event :guest-ids)
                                             (disj guest-id)
                                             first
                                             roles)]
                        (empty? (set/intersection my-role-prefs others-roles))))
                    {:factor/id :factor.id/without-matching-preferred-role
                     :factor/score 5}
                    ;; at preferred time
                    (and
                      availabilities
                      (contains? (availabilities guest-id) [(event :at) :preferred]))
                    {:factor/id :factor.id/at-preferred-time
                     :factor/score -5}
                    ;; at available time
                    (and
                      availabilities
                      (contains? (availabilities guest-id) [(event :at) :available]))
                    {:factor/id :factor.id/at-available-time
                     :factor/score -1}
                    :else
                    {:factor/id :factor.id/default
                     :factor/score 0})))))))


(defn individual-score
  [guest-id context]
  (->> (individual-score-meta guest-id context)
       (map :factor/score)
       (reduce +)))

(defn schedule-score
  [{:keys [schedule availabilities] :as context}]
  (->> availabilities
       keys
       (map (fn [guest-id]
              ;; create a non-linearity, to prefer scheduling an event for someone with fewer events than someone with many
              (let [guest-events (->> schedule
                                      (filter (fn [event]
                                                (contains? (event :guest-ids) guest-id))))
                    other-guest-count (->> guest-events
                                           (mapcat :guest-ids)
                                           set
                                           count
                                           dec)]
                (- (* (individual-score guest-id context)
                      (/ (inc (count guest-events)))
                      (Math/pow (count guest-events) 0.5))
                   other-guest-count))))
       (reduce +)))

(defn tweak-schedule
  [{:keys [schedule availabilities] :as context}]
  (case (if (empty? schedule)
          :create-event
          (rand-nth [:move-event :drop-event :create-event]))
    :create-event
    (let [guest-ids (take 2 (shuffle (keys availabilities)))
          event (random-event guest-ids context)]
      (if (nil? (:at event))
       context
       (update context :schedule conj event)))

    :drop-event
    (update context
            :schedule
            (fn [schedule]
              (let [event-index (rand-int (count schedule))]
                (remove-from-vec event-index (vec schedule)))))

    :move-event
    (update context
            :schedule
            (fn [schedule]
              (let [event-index (rand-int (count schedule))]
                (update (vec schedule) event-index
                        (fn [event]
                          (random-event (event :guest-ids) context))))))))

(defn optimize-schedule
  [{:keys [schedule availabilities report-fn] :as context}]
  (let [max-iterations 5000
        max-tweaks-per-iteration 4]
    (loop [context context
           iteration-count 0]
      (when report-fn
        (report-fn iteration-count (schedule-score context)))
      (if (> iteration-count max-iterations)
        context
        (let [tweak-count (+ 1 (rand-int max-tweaks-per-iteration))
              tweak-n-times (apply comp (repeat tweak-count tweak-schedule))
              alt-context (tweak-n-times context)]
          (if (< (schedule-score alt-context)
                 (schedule-score context))
            (recur alt-context (inc iteration-count))
            (recur context (inc iteration-count))))))))

(defn generate-initial-schedule
  [times-to-pair {:keys [availabilities] :as context}]
  (let [guest-ids (keys availabilities)
        pairs (combo/combinations guest-ids 2)]
    (assoc context
           :schedule
           (->> pairs
                (repeat times-to-pair)
                (apply concat)
                (map (fn [guest-ids]
                       (random-event guest-ids context)))
                (remove (fn [event] (nil? (:at event))))))))


(defn schedule
  [{:keys [availabilities times-to-pair] :as context}]
  (if (< (count availabilities) 2)
   (assoc context :schedule [])
   (-> (generate-initial-schedule times-to-pair context)
       optimize-schedule)))

(defn report
  [{:keys [schedule availabilities] :as context}]
  {:schedule schedule
   :event-count (count schedule)
   :score (double (schedule-score context))
   :guests (->> availabilities
                keys
                (map (fn [guest-id]
                       [guest-id (let [guest-events (->> schedule
                                                         (filter (fn [event]
                                                                   (contains? (event :guest-ids) guest-id))))]
                                   {:score (double (individual-score guest-id context))
                                    :count (count guest-events)
                                    :unique (count (disj (set (mapcat :guest-ids guest-events)) guest-id))})]))
                (into {}))})

(defn -main [])

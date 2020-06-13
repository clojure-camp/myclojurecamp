(ns pairing-scheduler.core
  (:require
   [clojure.math.combinatorics :as combo]
   [clojure.set :as set]))

(defn overlapping-daytimes
  [guest-ids {:keys [availabilities]}]
  (->> guest-ids
       (map availabilities)
       (map (fn [availabilities]
              (set (map (fn [[day-of-week hour _]]
                          [day-of-week hour]) availabilities))))
       (apply set/intersection)))

(defn random-event
  [guest-ids {:keys [availabilities] :as context}]
  (let [possible-times (overlapping-daytimes guest-ids context)]
    (if (seq possible-times)
      (let [[day-of-week time-of-day] (rand-nth (vec possible-times))]
        {:guest-ids (set guest-ids)
         :day-of-week day-of-week
         :time-of-day time-of-day})
      {:guest-ids (set guest-ids)
       :day-of-week :impossible
       :time-of-day -1})))

(defn remove-from-vec
  [pos coll]
  (vec (concat (subvec coll 0 pos) (subvec coll (inc pos)))))

(defn tweak-schedule
  [{:keys [schedule availabilities] :as context}]
  (case (if (empty? schedule)
          :create-event
          (rand-nth [:move-event :drop-event :create-event]))
    :create-event
    (update context
            :schedule
            conj (let [guest-ids (take 2 (shuffle (keys availabilities)))]
                   (random-event guest-ids context)))

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
                       (when-let [[day-of-week time-of-day] (first (overlapping-daytimes guest-ids context))]
                         {:guest-ids (set guest-ids)
                          :day-of-week :monday
                          :time-of-day 900})))
                (remove nil?)))))

(defn individual-score
  [guest-id {:keys [schedule availabilities]}]
  (let [max-events-per-day 2
        guest-events (->> schedule
                          (filter (fn [event]
                                    (contains? (event :guest-ids) guest-id))))
        guest-open-times  (->> (availabilities guest-id)
                               (map (fn [[day-of-week time-of-day _]]
                                      [day-of-week time-of-day]))
                               set)
        guest-event-times (->> guest-events
                               (map (fn [event]
                                      [(event :day-of-week) (event :time-of-day)])))]
    (->> guest-events
         (map (fn [event]
                ;; using negatives for ok events, to promote more events rather than fewer
                ;; b/c otherwise, an empty schedule would always be a perfect schedule
                (cond
                  ;; double-scheduled
                  (< 1 (->> guest-event-times
                            (filter (partial = [(event :day-of-week) (event :time-of-day)]))
                            count))
                  200
                  ;; outside of any available times
                  (not (contains? guest-open-times [(event :day-of-week) (event :time-of-day)]))
                  100
                  ;; above max for day
                  (< max-events-per-day
                     (->> guest-event-times
                          (filter (fn [[day-of-week _]]
                                    (= day-of-week (event :day-of-week))))
                          count))
                  50
                  ;; at preferred time
                  (contains? (availabilities guest-id) [(event :day-of-week) (event :time-of-day) :preferred])
                  -5
                  ;; at available time
                  (contains? (availabilities guest-id) [(event :day-of-week) (event :time-of-day) :available])
                  -1)))
         (reduce +))))

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

(defn optimize-schedule
  [{:keys [schedule availabilities] :as context}]
  (let [max-iterations 5000
        max-tweaks-per-iteration 2]
    (loop [context context
           iteration-count 0]
      (when (= 0 (mod iteration-count 1000))
        (println (double (schedule-score context))))
      (if (> iteration-count max-iterations)
        context
        (let [tweak-count (+ 1 (rand-int max-tweaks-per-iteration))
              tweak-n-times (apply comp (repeat tweak-count tweak-schedule))
              alt-context (tweak-n-times context)]
          (if (< (schedule-score alt-context)
                 (schedule-score context))
            (recur alt-context (inc iteration-count))
            (recur context (inc iteration-count))))))))

(defn report
  [{:keys [schedule availabilities] :as context}]
  {:schedule schedule
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

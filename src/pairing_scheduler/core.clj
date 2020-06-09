(ns pairing-scheduler.core
  (:require
   [clojure.math.combinatorics :as combo]
   [clojure.set :as set]))

(defn ->daytimes
  "Given map in the form {:monday [900 1000...] ...}
  returns a set in the form #{[:monday 900] [:monday 1000] ...}"
  [availability]
  (->> availability
       (mapcat (fn [[day-of-week hours]]
                 (map (fn [hour]
                           [day-of-week hour]) hours)))
       set))

(defn tweak-schedule
  [{:keys [schedule availabilities] :as context}]
  (assoc context
    :schedule
    (if (empty? schedule)
      schedule
      ;; move one event to another time
      (let [event-index (rand-int (count schedule))]
        (update (vec schedule) event-index
                (fn [event]
                  ;; select a daytime where both guests have availability
                  ;; (can still result in doublebooking)
                  (let [shared-daytimes (->> (event :guest-ids)
                                             (map availabilities)
                                             (map ->daytimes)
                                             (apply set/intersection))
                        [day-of-week time-of-day] (rand-nth (vec shared-daytimes))]
                    (assoc event
                      :day-of-week day-of-week
                      :time-of-day time-of-day))))))))

(defn generate-initial-schedule
  [{:keys [availabilities] :as context}]
  (let [times-to-pair 2
        guest-ids (keys availabilities)
        pairs (combo/combinations guest-ids 2)]
    (assoc context
      :schedule
      (->> pairs
           (map (fn [pair]
                  {:guest-ids (set pair)
                   :day-of-week :monday
                   :time-of-day 900}))
           set))))

(defn individual-score
  [guest-id {:keys [schedule availabilities]}]
  (+
    ;; avoid double-scheduling
    (let [daytimes (->> schedule
                        (filter (fn [event]
                                  (contains? (event :guest-ids) guest-id)))
                        (map (fn [event]
                               [(event :day-of-week) (event :time-of-day)])))
          n (- (count daytimes)
               (count (set daytimes)))]
      (* 100 n))

    ;; avoid scheduling outside available times
    (->> schedule
         (filter (fn [event]
                   (contains? (event :guest-ids) guest-id)))
         (remove (fn [event]
                   (contains? (get-in availabilities [guest-id (event :day-of-week)])
                              (event :time-of-day))))
         count
         (* 200))))

(defn schedule-score
  [{:keys [schedule availabilities] :as context}]
  (->> availabilities
       keys
       (map (fn [guest-id]
               (individual-score guest-id context)))
       (reduce +)))

(defn optimize-schedule
  [{:keys [schedule availabilities] :as context}]
  (let [max-iterations 5000
        max-tweaks-per-iteration 2]
    (loop [context context
           iteration-count 0]
      (when (= 0 (mod iteration-count 1000))
        (println (schedule-score context)))
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
  {:score (schedule-score context)
   :scores (->> availabilities
                keys
                (map (fn [guest-id]
                       [guest-id (individual-score guest-id context)]))
                (into {}))
   :schedule schedule
   :availabilities availabilities})

#_(->> {:availabilities {"raf" {:monday #{1000 1100}}
                         "dh" {:monday #{1000 1100 1200}}
                         "berk" {:monday #{1100 1200}}}}
       generate-initial-schedule
       optimize-schedule
       report
       clojure.pprint/pprint)

(defn -main []
  (println "hello world"))


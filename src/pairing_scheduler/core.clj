(ns pairing-scheduler.core
  (:require
   [clojure.math.combinatorics :as combo]
   [clojure.set :as set]))

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
                                             (map (fn [availabilities]
                                                    (set (map (fn [[day-of-week hour _]]
                                                                [day-of-week hour]) availabilities))))
                                             (apply set/intersection))
                        [day-of-week time-of-day] (rand-nth (vec shared-daytimes))]
                    (assoc event
                      :day-of-week day-of-week
                      :time-of-day time-of-day))))))))

(defn generate-initial-schedule
  [times-to-pair {:keys [availabilities] :as context}]
  (let [guest-ids (keys availabilities)
        pairs (combo/combinations guest-ids 2)]
    (assoc context
      :schedule
      (->> pairs
           (repeat times-to-pair)
           (apply concat)
           (map (fn [pair]
                  {:guest-ids (set pair)
                   :day-of-week :monday
                   :time-of-day 900}))))))

(defn individual-score
  [guest-id {:keys [schedule availabilities]}]
  (let [guest-events (->> schedule
                          (filter (fn [event]
                                    (contains? (event :guest-ids) guest-id))))]
    (+
     ;; penalize events that are double-scheduled for the guest
     (let [daytimes (->> guest-events
                         (map (fn [event]
                                [(event :day-of-week) (event :time-of-day)])))
           n (- (count daytimes)
                (count (set daytimes)))]
       (* 100 n))

     ;; penalize events outside of guest's available times
     (->> guest-events
          (remove (fn [event]
                    (or (contains? (availabilities guest-id) [(event :day-of-week) (event :time-of-day) :available])
                        (contains? (availabilities guest-id) [(event :day-of-week) (event :time-of-day) :preferred]))))
          count
          (* 200))

     ;; penalize events during available times slightly (to bias towards preferred times)
     (->> guest-events
          (filter (fn [event]
                    (contains? (availabilities guest-id) [(event :day-of-week) (event :time-of-day) :available])))
          count
          (* 1)))))

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
  (assoc context
    :score (schedule-score context)
    :scores (->> availabilities
                 keys
                 (map (fn [guest-id]
                        [guest-id (individual-score guest-id context)]))
                 (into {}))))

#_(->> {:availabilities {"raf" #{[:monday 1000 :preferred]
                                 [:monday 1100 :preferred]}
                         "dh" #{[:monday 1000 :available]
                                [:monday 1100 :available]
                                [:monday 1200 :available]}
                         "berk" #{[:monday 1100 :available]
                                  [:monday 1200 :available]}}}
       (generate-initial-schedule 1)
       optimize-schedule
       report
       clojure.pprint/pprint)

(defn -main []
  (println "hello world"))

#_(do
    (def context
      {:availabilities
       {"raf" {:monday #{1300 1400 1500}
               :tuesday #{}
               :wednesday #{1000 1100 1200 1300 1400 1500 1600}
               :thursday #{1000 1100 1200 1300 1400 1500 1600}
               :friday #{1000 1100 1200 1300 1400 1500 1600}}
        "dh" {:monday #{1300 1400 1500 1600}
              :tuesday #{1300 1400 1500 1600}
              :wednesday #{1300 1400 1500 1600}
              :thursday #{1300 1400 1500 1600}
              :friday #{1300 1400 1500 1600}}
        "berk" {:monday #{900 1000 1100 1200 1300 1400 1500 1600}
                :tuesday #{900 1000 1100 1200 1300 1400 1500 1600}
                :wednesday #{900 1000 1100 1200 1300 1400 1500 1600}
                :thursday #{900 1000 1100 1200 1300 1400 1500 1600}
                :friday #{900 1000 1100 1200 1300 1400 1500 1600}}
        "james" {:monday #{1200 1300 1400 1500 1600}
                 :tuesday #{1000 1100}
                 :wednesday #{1000 1100 1200 1300 1400 1500 1600}
                 :thursday #{1000 1100}
                 :friday #{1000 1100}}}})
    (def context (generate-initial-schedule 2 context))
    (do
      (def context (optimize-schedule context))
      (clojure.pprint/pprint (report context))))

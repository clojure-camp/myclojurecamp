(ns pairing-scheduler.core
  (:require
   [clojure.math.combinatorics :as combo]))

(defn tweak-schedule [schedule]
  (if (empty? schedule)
    schedule
    ;; move one event to another time
    (let [event-index (rand-int (count schedule))]
      (update (vec schedule) event-index
              (fn [event]
                (assoc event
                       :day-of-week (rand-nth [:monday :tuesday :wednesday :thursday :friday])
                       :time-of-day (rand-nth [900 1000 1100 1200 1300 1400 1500 1600 1700])))))))

(defn generate-initial-schedule [availabilities]
  (let [guest-ids (keys availabilities)
        pairs (combo/combinations guest-ids 2)]
    (->> pairs
         (map (fn [pair]
                {:guest-ids (set pair)
                 :day-of-week :monday
                 :time-of-day 900}))
         set)))

(defn individual-score [guest-id schedule availabilities]
  (cond
    ;; no double-scheduling
    (->> schedule
         (filter (fn [event]
                   (contains? (event :guest-ids) guest-id)))
         (map (fn [event]
                [(event :day-of-week) (event :time-of-day)]))
         (apply distinct?)
         not)
    999

    ;; always within available times
    (->> schedule
         (filter (fn [event]
                   (contains? (event :guest-ids) guest-id)))
         (every? (fn [event]
                   (contains? (get-in availabilities [guest-id (event :day-of-week)])
                              (event :time-of-day))))
         not)
    999

    :else
    0))

(defn schedule-score [schedule availabilities]
  (->> availabilities
       keys
       (map (fn [guest-id]
               (individual-score guest-id schedule availabilities)))
       (reduce +)))

(defn optimize-schedule [schedule availabilities]
  (let [max-iterations 5000
        max-tweaks-per-iteration 2]
    (loop [schedule schedule
           iteration-count 0]
      (when (= 0 (mod iteration-count 100))
        (println (schedule-score schedule availabilities)))
      (if (> iteration-count max-iterations)
        schedule
        (let [tweak-count (+ 1 (rand-int max-tweaks-per-iteration))
              tweak-schedule-n-times (apply comp (repeat tweak-count tweak-schedule))
              alt-schedule (tweak-schedule-n-times schedule)]
          (if (< (schedule-score alt-schedule availabilities)
                 (schedule-score schedule availabilities))
            (recur alt-schedule (inc iteration-count))
            (recur schedule (inc iteration-count))))))))

#_(let [availabilities {"raf" {:monday #{1000 1100}}
                        "dh" {:monday #{1000 1100 1200}}
                        "berk" {:monday #{1100 1200}}}
        optimized-schedule (optimize-schedule
                            (generate-initial-schedule availabilities)
                            availabilities)]
    {:score (schedule-score optimized-schedule availabilities)
     :scores (->> availabilities
                  keys
                  (map (fn [guest-id] (individual-score guest-id optimized-schedule availabilities))))
     :schedule optimized-schedule})

(defn -main []
  (println "hello world"))


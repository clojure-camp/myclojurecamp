(ns pairing-scheduler.import
  (:require
   [clojure.string :as string]
   [clojure.data.csv :as csv]
   [clojure.java.io :as io]))

(defn update-available-to-preferred
  "If the user has only :available timeslots, change all to :preferred"
  [availabilities]
  (->> availabilities
       (map (fn [[guest-id guest-availabilities]]
              (if (->> guest-availabilities
                       (map last)
                       (apply = :available))
                  [guest-id
                   (->> guest-availabilities
                        (map (fn [[inst _]]
                               [inst :preferred]))
                        set)]
                  [guest-id guest-availabilities])))
       (into {})))

(defn read-csv [file-path]
  (with-open [reader (io/reader file-path)]
    (doall
     (csv/read-csv reader))))

(defn ->availabilities [raw-availabilities]
  (let [times (for [day-of-week [:monday :tuesday :wednesday :thursday :friday]
                    hour-of-day [900 1000 1100 1200 1300 1400 1500 1600 1700]]
                [day-of-week hour-of-day])]
    (->> (map conj
              times
              (->> raw-availabilities
                   (map {"" nil
                         "P" :preferred
                         "A" :available})))
         (filter (fn [[_ _ availability]]
                   availability))
         set)))

(defn read-schedule
  [file-path]
  (let [default-max-hours-per-day 2
        stuff (->> (read-csv file-path)
                   (drop 3)
                   (map (fn [row]
                          {:name (row 0)
                           :max-hours-per-day (if (string/blank? (row 2))
                                                default-max-hours-per-day
                                                (Integer. (row 2)))
                           :availabilities (->availabilities (drop 3 row))})))]
    {:availabilities (->> stuff
                          (map (fn [{:keys [name availabilities]}]
                                 [name availabilities]))
                          update-available-to-preferred
                          (into {}))
     :max-events-per-day (->> stuff
                              (map (fn [{:keys [name max-hours-per-day]}]
                                     [name max-hours-per-day]))
                              (into {}))}))

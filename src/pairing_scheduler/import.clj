(ns pairing-scheduler.import
  (:require
   [clojure.string :as string]
   [clojure.data.csv :as csv]
   [clojure.java.io :as io]))

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
  (->> (read-csv file-path)
       (drop 2)
       (map (fn [row]
              {:name (row 0)
               :max-pair-hours-per-day (if (string/blank? (row 2))
                                         nil
                                         (Integer. (row 2)))
               :availabilities (->availabilities (drop 3 row))}))
       (map (fn [{:keys [name availabilities]}]
              [name availabilities]))
       (into {})))

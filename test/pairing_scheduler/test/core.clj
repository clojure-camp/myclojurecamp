(ns pairing-scheduler.test.core
  (:require
   [pairing-scheduler.core :as ps]
   [clojure.test :refer [deftest testing is]]))

;; schedule
[{:guest-ids #{"raf" "dh"}
  :day-of-week :wednesday
  :time-of-day 1500}]

;; availabilities

{"Raf" {:monday #{900 1000 1100}
        :tuesday #{900}}
 "DH" {:monday #{900 1000 1100}
       :tuesday #{900}}}

(deftest scheduler
  (testing "scoring-fn double-scheduling"
    (is (= 999
           (ps/individual-score
            "raf"
            [{:guest-ids #{"raf" "dh"}
              :day-of-week :wednesday
              :time-of-day 1500}
             {:guest-ids #{"raf" "berk"}
              :day-of-week :wednesday
              :time-of-day 1500}]
            {}))))

  (testing "scoring-fn always-within-available-times"
    (is (= 999
           (ps/individual-score
            "raf"
            [{:guest-ids #{"raf" "dh"}
              :day-of-week :wednesday
              :time-of-day 1800}]
            {"raf" {}}))))

  (testing "scoring-fn not-within-available-times"
    (is (= 0
           (ps/individual-score
            "raf"
            [{:guest-ids #{"raf" "dh"}
              :day-of-week :monday
              :time-of-day 900}]
            {"raf" {:monday #{900}}
             "dh" {:monday #{900}}}))))

  (testing "generate-initial-schedule"
    (is (= #{{:guest-ids #{"Raf" "DH"}
              :day-of-week :monday
              :time-of-day 900}
             {:guest-ids #{"Berk" "DH"}
              :day-of-week :monday
              :time-of-day 900}
             {:guest-ids #{"Berk" "Raf"}
              :day-of-week :monday
              :time-of-day 900}}
           (ps/generate-initial-schedule
            {"Raf" {}
             "Berk" {}
             "DH" {}}))))

  (testing "optimize-schedule"
    (let [availabilities {"raf" {:monday #{1000 1100}}
                          "dh" {:monday #{1000 1100 1200}}
                          "berk" {:monday #{1100 1200}}}]
      (is (=
           (set [{:guest-ids #{"raf" "dh"}
                  :day-of-week :monday
                  :time-of-day 1000}
                 {:guest-ids #{"raf" "berk"}
                  :day-of-week :monday
                  :time-of-day 1100}
                 {:guest-ids #{"berk" "dh"}
                  :day-of-week :monday
                  :time-of-day 1200}]))
          (set
           (ps/optimize-schedule (ps/generate-initial-schedule availabilities)
                                 availabilities))))))
(ns pairing-scheduler.test.core
  (:require
   [pairing-scheduler.core :as ps]
   [clojure.test :refer [deftest testing is]]))

;; schedule
[{:guest-ids #{"raf" "dh"}
  :day-of-week :wednesday
  :time-of-day 1500}]

;; availabilities

{"Raf" #{[:monday 900 :available]
         [:monday 1000 :preferred]}
 "DH" #{}}

(deftest individual-score
  (testing "double-scheduling"
    (is (= 400
           (ps/individual-score
            "raf"
            {:schedule
             [{:guest-ids #{"raf" "dh"}
               :day-of-week :wednesday
               :time-of-day 1500}
              {:guest-ids #{"raf" "berk"}
               :day-of-week :wednesday
               :time-of-day 1500}]
             :availabilities
             {"raf" #{[:wednesday 1500 :preferred]}}}))))

  (testing "not within available times"
    (is (= 100
           (ps/individual-score
            "raf"
            {:schedule
             [{:guest-ids #{"raf" "dh"}
               :day-of-week :wednesday
               :time-of-day 1800}]
             :availabilities
             {"raf" #{}}}))))

  (testing "above max-events-per-day"
    (is (= 150
           (ps/individual-score
            "raf"
            {:schedule
             [{:guest-ids #{"raf" "dh"}
               :day-of-week :monday
               :time-of-day 900}
              {:guest-ids #{"raf" "dh"}
               :day-of-week :monday
               :time-of-day 1000}
              {:guest-ids #{"raf" "dh"}
               :day-of-week :monday
               :time-of-day 1100}]
             :availabilities
             {"raf" #{[:monday 900 :available]
                      [:monday 1000 :available]
                      [:monday 1100 :available]}
              "dh" #{[:monday 900 :available]
                     [:monday 1000 :available]
                     [:monday 1100 :available]}}}))))

  (testing "within available times"
    (is (= -1
           (ps/individual-score
            "raf"
            {:schedule
             [{:guest-ids #{"raf" "dh"}
               :day-of-week :monday
               :time-of-day 900}]
             :availabilities
             {"raf" #{[:monday 900 :available]}
              "dh" #{[:monday 900 :available]}}}))))

  (testing "within preferred times"
    (is (= -5
           (ps/individual-score
            "raf"
            {:schedule
             [{:guest-ids #{"raf" "dh"}
               :day-of-week :monday
               :time-of-day 900}]
             :availabilities
             {"raf" #{[:monday 900 :preferred]}
              "dh" #{[:monday 900 :preferred]}}})))))

(deftest overlapping-daytimes
  (testing "some overlap"
    (is (= #{[:monday 900]}
           (ps/overlapping-daytimes
            #{"Raf" "Berk"}
            {:availabilities
             {"Raf" #{[:monday 900 :available]}
              "Berk" #{[:monday 900 :preferred]}}}))))

  (testing "no overlap"
    (is (= #{}
           (ps/overlapping-daytimes
            #{"Raf" "Berk"}
            {:availabilities
             {"Raf" #{[:monday 900 :available]}
              "Berk" #{[:tuesday 900 :preferred]}}})))))

(deftest generate-initial-schedule
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
           (->> (ps/generate-initial-schedule
                 1
                 {:availabilities
                  {"Raf" #{[:monday 900 :available]}
                   "Berk" #{[:monday 900 :available]}
                   "DH" #{[:monday 900 :available]}}})
                :schedule
                set)))))

(deftest schedule-score
  (testing "prefer distributing events amongst users"
    (let [availabilities {"raf" #{[:monday 1000 :available]
                                  [:tuesday 1000 :available]
                                  [:thursday 1000 :available]}
                          "dh" #{[:monday 1000 :available]
                                 [:tuesday 1000 :available]
                                 [:thursday 1000 :available]}
                          "berk" #{[:monday 1000 :available]
                                   [:tuesday 1000 :available]
                                   [:thursday 1000 :available]}}]
      (is (< (ps/schedule-score
              {:availabilities availabilities
               :schedule
               [{:guest-ids #{"dh" "berk"}
                 :day-of-week :monday
                 :time-of-day 1000}
                {:guest-ids #{"raf" "berk"}
                 :day-of-week :tuesday
                 :time-of-day 1000}
                {:guest-ids #{"dh" "raf"}
                 :day-of-week :thursday
                 :time-of-day 1000}]})
             (ps/schedule-score
              {:availabilities availabilities
               :schedule
               [{:guest-ids #{"dh" "berk"}
                 :day-of-week :monday
                 :time-of-day 1000}
                {:guest-ids #{"dh" "berk"}
                 :day-of-week :thursday
                 :time-of-day 1000}
                {:guest-ids #{"dh" "berk"}
                 :day-of-week :tuesday
                 :time-of-day 1000}]})))))

  (testing "prefer a variety of guests per guest"
    (let [availabilities {"raf" #{[:monday 1000 :available]
                                  [:monday 1100 :available]}
                          "dh" #{[:monday 1000 :available]
                                 [:monday 1100 :available]}
                          "berk" #{[:monday 1000 :available]
                                   [:monday 1100 :available]}
                          "james" #{[:monday 1000 :available]
                                    [:monday 1100 :available]}}]
      (is (< (ps/schedule-score
              {:availabilities availabilities
               :schedule
               [{:guest-ids #{"raf" "dh"}
                 :day-of-week :monday
                 :time-of-day 1000}
                {:guest-ids #{"raf" "berk"}
                 :day-of-week :monday
                 :time-of-day 1100}
                {:guest-ids #{"james" "berk"}
                 :day-of-week :monday
                 :time-of-day 1000}
                {:guest-ids #{"james" "dh"}
                 :day-of-week :monday
                 :time-of-day 1100}]})
             (ps/schedule-score
              {:availabilities availabilities
               :schedule
               [{:guest-ids #{"raf" "dh"}
                 :day-of-week :monday
                 :time-of-day 1000}
                {:guest-ids #{"raf" "dh"}
                 :day-of-week :monday
                 :time-of-day 1100}
                {:guest-ids #{"james" "berk"}
                 :day-of-week :monday
                 :time-of-day 1000}
                {:guest-ids #{"james" "berk"}
                 :day-of-week :monday
                 :time-of-day 1100}]}))))))

(deftest optimize-schedule
  (testing "basic"
    (is (= (set [{:guest-ids #{"raf" "dh"}
                  :day-of-week :monday
                  :time-of-day 1000}
                 {:guest-ids #{"raf" "berk"}
                  :day-of-week :monday
                  :time-of-day 1100}
                 {:guest-ids #{"berk" "dh"}
                  :day-of-week :monday
                  :time-of-day 1200}])
           (->> {:availabilities
                 {"raf" #{[:monday 1000 :available]
                          [:monday 1100 :available]}
                  "dh" #{[:monday 1000 :available]
                         [:monday 1100 :available]
                         [:monday 1200 :available]}
                  "berk" #{[:monday 1100 :available]
                           [:monday 1200 :available]}}}
                (ps/generate-initial-schedule 1)
                ps/optimize-schedule
                :schedule
                set))))

  (testing "empty schedule if no overlap possible between guests"
    (is (= #{}
           (->> {:availabilities
                 {"raf" #{}
                  "dh" #{[:monday 1000 :preferred]
                         [:monday 1200 :preferred]}}
                 :schedule [{:guest-ids #{"raf" "dh"}
                             :day-of-week :monday
                             :time-of-day 900}]}
                ps/optimize-schedule
                :schedule
                set))))

  (testing "prefer scheduling during preferred times"
    (is (= #{{:guest-ids #{"raf" "dh"}
              :day-of-week :monday
              :time-of-day 1000}
             {:guest-ids #{"raf" "berk"}
              :day-of-week :monday
              :time-of-day 1100}
             {:guest-ids #{"berk" "dh"}
              :day-of-week :monday
              :time-of-day 1200}}
           (->> {:availabilities
                 {"raf" #{[:monday 1000 :preferred]
                          [:monday 1100 :preferred]
                          [:monday 1200 :available]}
                  "dh" #{[:monday 1000 :preferred]
                         [:monday 1100 :available]
                         [:monday 1200 :preferred]}
                  "berk" #{[:monday 1000 :available]
                           [:monday 1100 :preferred]
                           [:monday 1200 :preferred]}}}
                (ps/generate-initial-schedule 1)
                ps/optimize-schedule
                :schedule
                set))))

  (testing "prefer evenly spreading events amongst users"
    (let [schedule (->> {:availabilities
                         {"raf" #{[:monday 1000 :available]
                                  [:tuesday 1000 :available]
                                  [:thursday 1000 :available]}
                          "dh" #{[:monday 1000 :available]
                                 [:tuesday 1000 :available]
                                 [:thursday 1000 :available]}
                          "berk" #{[:monday 1000 :available]
                                   [:tuesday 1000 :available]
                                   [:thursday 1000 :available]}}}
                        (ps/generate-initial-schedule 1)
                        ps/optimize-schedule
                        :schedule)
          events-per-person (reduce (fn [memo guest-id]
                                      (assoc memo guest-id
                                             (count (filter (fn [event]
                                                              (contains? (event :guest-ids) guest-id)) schedule)))) {} ["raf" "dh" "berk"])]
      (is (= {"raf" 2
              "dh" 2
              "berk" 2}
             events-per-person)))))

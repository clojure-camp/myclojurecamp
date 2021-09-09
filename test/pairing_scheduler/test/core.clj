(ns pairing-scheduler.test.core
  (:require
   [pairing-scheduler.core :as ps]
   [pairing-scheduler.import :as ps.import]
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
    (is (= 47
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
             :max-events-per-day
             {"raf" 2
              "dh" 2}
             :availabilities
             {"raf" #{[:monday 900 :available]
                      [:monday 1000 :available]
                      [:monday 1100 :available]}
              "dh" #{[:monday 900 :available]
                     [:monday 1000 :available]
                     [:monday 1100 :available]}}}))))

  (testing "above max-events-per-week"
    (is (= 97
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
             :max-events-per-week
             {"raf" 2
              "dh" 2}
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
  (testing "prefers empty schedule to overlap"
    (let [context {:availabilities
                   {"raf" #{}
                    "dh" #{[:monday 1000 :preferred]
                           [:monday 1200 :preferred]}}}]
      (is (< (ps/schedule-score
              (assoc context :schedule []))
             (ps/schedule-score
              (assoc context :schedule [{:guest-ids #{"raf" "dh"}
                                         :day-of-week :monday
                                         :time-of-day 900}]))))))

  (testing "prefer distributing events amongst users"
    (let [context {:availabilities
                   {"raf" #{[:monday 1000 :available]
                            [:tuesday 1000 :available]
                            [:thursday 1000 :available]}
                    "dh" #{[:monday 1000 :available]
                           [:tuesday 1000 :available]
                           [:thursday 1000 :available]}
                    "berk" #{[:monday 1000 :available]
                             [:tuesday 1000 :available]
                             [:thursday 1000 :available]}}}]
      (is (< (ps/schedule-score
              (assoc context :schedule
                     [{:guest-ids #{"dh" "berk"}
                       :day-of-week :monday
                       :time-of-day 1000}
                      {:guest-ids #{"raf" "berk"}
                       :day-of-week :tuesday
                       :time-of-day 1000}
                      {:guest-ids #{"dh" "raf"}
                       :day-of-week :thursday
                       :time-of-day 1000}]))
             (ps/schedule-score
              (assoc context :schedule
                     [{:guest-ids #{"dh" "berk"}
                       :day-of-week :monday
                       :time-of-day 1000}
                      {:guest-ids #{"dh" "berk"}
                       :day-of-week :thursday
                       :time-of-day 1000}
                      {:guest-ids #{"dh" "berk"}
                       :day-of-week :tuesday
                       :time-of-day 1000}]))))))

  (testing "prefer a variety of guests per guest"
    (let [context {:availabilities
                   {"raf" #{[:monday 1000 :available]
                            [:monday 1100 :available]}
                    "dh" #{[:monday 1000 :available]
                           [:monday 1100 :available]}
                    "berk" #{[:monday 1000 :available]
                             [:monday 1100 :available]}
                    "james" #{[:monday 1000 :available]
                              [:monday 1100 :available]}}}]
      (is (< (ps/schedule-score
              (assoc context :schedule
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
                       :time-of-day 1100}]))
             (ps/schedule-score
              (assoc context :schedule
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
                       :time-of-day 1100}])))))))

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

(deftest update-available-to-preferred
  (testing "user has all timeslots set to available"
    (is (= {"raf" #{[:monday 1000 :available]
                    [:tuesday 1000 :available]
                    [:thursday 1000 :preferred]}
            "dh" #{[:monday 1000 :preferred]
                   [:tuesday 1000 :preferred]
                   [:thursday 1000 :preferred]}
            "berk" #{[:monday 1000 :preferred]
                     [:tuesday 1000 :available]
                     [:thursday 1000 :available]}}
           (ps.import/update-available-to-preferred
            {"raf" #{[:monday 1000 :available]
                     [:tuesday 1000 :available]
                     [:thursday 1000 :preferred]}
             "dh" #{[:monday 1000 :available]
                    [:tuesday 1000 :available]
                    [:thursday 1000 :available]}
             "berk" #{[:monday 1000 :preferred]
                      [:tuesday 1000 :available]
                      [:thursday 1000 :available]}}))))

  (testing "all users have no preferred time"
    (is (= {"raf" #{[:monday 1000 :preferred]
                    [:tuesday 1000 :preferred]
                    [:thursday 1000 :preferred]}
            "dh" #{[:monday 1000 :preferred]
                   [:tuesday 1000 :preferred]
                   [:thursday 1000 :preferred]}
            "berk" #{[:monday 1000 :preferred]
                     [:tuesday 1000 :preferred]
                     [:thursday 1000 :preferred]}}
           (ps.import/update-available-to-preferred
            {"raf" #{[:monday 1000 :available]
                     [:tuesday 1000 :available]
                     [:thursday 1000 :available]}
             "dh" #{[:monday 1000 :available]
                    [:tuesday 1000 :available]
                    [:thursday 1000 :available]}
             "berk" #{[:monday 1000 :available]
                      [:tuesday 1000 :available]
                      [:thursday 1000 :available]}})))))

(deftest schedule
  (testing "when no users, returns empty schedule"
    (is (= #{}
           (->> {:availabilities {}
                 :times-to-pair 1}
                ps/schedule
                :schedule
                set))))

  (testing "when single user, returns empty schedule"
   (is (= #{}
          (->> {:availabilities {"alice" #{}}
                :times-to-pair 1}
               ps/schedule
               :schedule
               set))))

  (testing "when no available times, returns empty schedule"
   (is (= #{}
          (->> {:availabilities {"alice" #{}
                                 "bob" #{}}
                :times-to-pair 1}
               ps/schedule
               :schedule
               set))))

  (testing "when no overlapping times, returns empty schedule"
   (is (= #{}
          (->> {:availabilities {"alice" #{[:monday 1000 :available]}
                                 "bob" #{[:monday 1100 :available]}}
                :times-to-pair 1}
               ps/schedule
               :schedule
               set))))

  (testing "when 3 users with limited availability, pairs 2 of them, and not 3rd"
    (is (= 1
           (->> {:max-events-per-day {"alice" 1
                                      "bob" 1
                                      "cathy" 1}
                 :availabilities {"alice" #{[:monday 1000 :available]}
                                  "bob" #{[:monday 1000 :available]}
                                  "cathy" #{[:monday 1000 :available]}}
                 :times-to-pair 1}
                ps/schedule
                :schedule
                set
                count))))

  (testing "does not schedule more than per-week-limit"
    (is (= 1
           (->> {:max-events-per-week {"alice" 1}
                 :availabilities {"alice" #{[:monday 1000 :available]
                                            [:tuesday 1000 :available]}
                                  "bob" #{[:monday 1000 :available]}
                                  "cathy" #{[:tuesday 1000 :available]}}
                 :times-to-pair 1}
                ps/schedule
                :schedule
                set
                count))))

  (testing "takes topics into consideration"
    (is (= 99
           (ps/individual-score
              "raf"
              {:schedule
               [{:guest-ids #{"raf" "dh"}
                 :day-of-week :wednesday
                 :time-of-day 1500}]
               :topics {"raf" #{3}
                        "dh" #{1 2}}
               :availabilities
               {"raf" #{[:wednesday 1500 :preferred]}}})))
    (is (= #{#{{:guest-ids #{"alice" "bob"}
                :day-of-week :monday
                :time-of-day 1000}
               {:guest-ids #{"cathy" "donald"}
                :day-of-week :monday
                :time-of-day 1000}}}
          (set (repeatedly 10
                (fn []
                  (->> {:availabilities {"alice" #{[:monday 1000 :available]}
                                         "bob" #{[:monday 1000 :available]}
                                         "cathy" #{[:monday 1000 :available]}
                                         "donald" #{[:monday 1000 :available]}}
                        :topics {"alice" #{"a" "b"}
                                 "bob" #{"a" "z"}
                                 "cathy" #{"e" "f"}
                                 "donald" #{"e" "g"}}
                        :times-to-pair 1}
                       ps/schedule
                       :schedule
                       set))))))))

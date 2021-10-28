(ns pairing-scheduler.test.core
  (:require
   [pairing-scheduler.core :as ps]
   [pairing-scheduler.import :as ps.import]
   [clojure.test :refer [deftest testing is]]))

;; schedule
[{:guest-ids #{"raf" "dh"}
  :at #inst "2021-01-01T09"}]

;; availabilities

{"Raf" #{[#inst "2021-01-01T09" :available]
         [#inst "2021-01-01T10" :preferred]}
 "DH" #{}}


(deftest individual-score
  (testing "double-scheduling"
    (is (= 400
           (ps/individual-score
            "raf"
            {:schedule
             [{:guest-ids #{"raf" "dh"}
               :at #inst "2021-01-01T09"}
              {:guest-ids #{"raf" "berk"}
               :at #inst "2021-01-01T09"}]
             :availabilities
             {"raf" #{[#inst "2021-01-01T09" :preferred]}}}))))

  (testing "not within available times"
    (is (= 100
           (ps/individual-score
            "raf"
            {:schedule
             [{:guest-ids #{"raf" "dh"}
               :at #inst "2021-01-01T09"}]
             :availabilities
             {"raf" #{}}}))))

  (testing "above max-events-per-day"
    (is (= 47
           (ps/individual-score
            "raf"
            {:schedule
             [{:guest-ids #{"raf" "dh"}
               :at #inst "2021-01-01T09"}
              {:guest-ids #{"raf" "dh"}
               :at #inst "2021-01-01T10"}
              {:guest-ids #{"raf" "dh"}
               :at #inst "2021-01-01T11"}]
             :timezones
             {"raf" "America/Toronto"
              "dh" "America/Toronto"}
             :max-events-per-day
             {"raf" 2
              "dh" 2}
             :availabilities
             {"raf" #{[#inst "2021-01-01T09" :available]
                      [#inst "2021-01-01T10" :available]
                      [#inst "2021-01-01T11" :available]}
              "dh" #{[#inst "2021-01-01T09" :available]
                     [#inst "2021-01-01T10" :available]
                     [#inst "2021-01-01T11" :available]}}}))))

  (testing "above max-events-per-week"
    (is (= 97
           (ps/individual-score
            "raf"
            {:schedule
             [{:guest-ids #{"raf" "dh"}
               :at #inst "2021-01-01T09"}
              {:guest-ids #{"raf" "dh"}
               :at #inst "2021-01-01T10"}
              {:guest-ids #{"raf" "dh"}
               :at #inst "2021-01-01T11"}]
             :max-events-per-week
             {"raf" 2
              "dh" 2}
             :availabilities
             {"raf" #{[#inst "2021-01-01T09" :available]
                      [#inst "2021-01-01T10" :available]
                      [#inst "2021-01-01T11" :available]}
              "dh" #{[#inst "2021-01-01T09" :available]
                     [#inst "2021-01-01T10" :available]
                     [#inst "2021-01-01T11" :available]}}}))))

  (testing "within available times"
    (is (= -1
           (ps/individual-score
            "raf"
            {:schedule
             [{:guest-ids #{"raf" "dh"}
               :at #inst "2021-01-01T09"}]
             :availabilities
             {"raf" #{[#inst "2021-01-01T09" :available]}
              "dh" #{[#inst "2021-01-01T09" :available]}}}))))

  (testing "within preferred times"
    (is (= -5
           (ps/individual-score
            "raf"
            {:schedule
             [{:guest-ids #{"raf" "dh"}
               :at #inst "2021-01-01T09"}]
             :availabilities
             {"raf" #{[#inst "2021-01-01T09" :preferred]}
              "dh" #{[#inst "2021-01-01T09" :preferred]}}})))))

(deftest overlapping-daytimes
  (testing "some overlap"
    (is (= #{#inst "2021-01-01T09"}
           (ps/overlapping-daytimes
            #{"Raf" "Berk"}
            {:availabilities
             {"Raf" #{[#inst "2021-01-01T09" :available]}
              "Berk" #{[#inst "2021-01-01T09" :preferred]}}}))))

  (testing "no overlap"
    (is (= #{}
           (ps/overlapping-daytimes
            #{"Raf" "Berk"}
            {:availabilities
             {"Raf" #{[#inst "2021-01-01T09" :available]}
              "Berk" #{[#inst "2021-01-02T09" :preferred]}}})))))

(deftest generate-initial-schedule
  (testing "generate-initial-schedule"
    (is (= #{{:guest-ids #{"Raf" "DH"}
              :at #inst "2021-01-01T09"}
             {:guest-ids #{"Berk" "DH"}
              :at #inst "2021-01-01T09"}
             {:guest-ids #{"Berk" "Raf"}
              :at #inst "2021-01-01T09"}}
           (->> (ps/generate-initial-schedule
                 1
                 {:availabilities
                  {"Raf" #{[#inst "2021-01-01T09" :available]}
                   "Berk" #{[#inst "2021-01-01T09" :available]}
                   "DH" #{[#inst "2021-01-01T09" :available]}}})
                :schedule
                set)))))

(deftest schedule-score
  (testing "prefers empty schedule to overlap"
    (let [context {:availabilities
                   {"raf" #{}
                    "dh" #{[#inst "2021-01-01T10" :preferred]
                           [#inst "2021-01-01T12" :preferred]}}}]
      (is (< (ps/schedule-score
              (assoc context :schedule []))
             (ps/schedule-score
              (assoc context :schedule [{:guest-ids #{"raf" "dh"}
                                         :at #inst "2021-01-01T09"}]))))))

  (testing "prefer distributing events amongst users"
    (let [context {:availabilities
                   {"raf" #{[#inst "2021-01-01T10" :available]
                            [#inst "2021-01-02T10" :available]
                            [#inst "2021-01-04T10" :available]}
                    "dh" #{[#inst "2021-01-01T10" :available]
                           [#inst "2021-01-02T10" :available]
                           [#inst "2021-01-04T10" :available]}
                    "berk" #{[#inst "2021-01-01T10" :available]
                             [#inst "2021-01-02T10" :available]
                             [#inst "2021-01-04T10" :available]}}}]
      (is (< (ps/schedule-score
              (assoc context :schedule
                     [{:guest-ids #{"dh" "berk"}
                       :at #inst "2021-01-01T10"}
                      {:guest-ids #{"raf" "berk"}
                       :at #inst "2021-01-02T10"}
                      {:guest-ids #{"dh" "raf"}
                       :at #inst "2021-01-04T10"}]))
             (ps/schedule-score
              (assoc context :schedule
                     [{:guest-ids #{"dh" "berk"}
                       :at #inst "2021-01-01T10"}
                      {:guest-ids #{"dh" "berk"}
                       :at #inst "2021-01-04T10"}
                      {:guest-ids #{"dh" "berk"}
                       :at #inst "2021-01-02T10"}]))))))

  (testing "prefer a variety of guests per guest"
    (let [context {:availabilities
                   {"raf" #{[#inst "2021-01-01T10" :available]
                            [#inst "2021-01-01T11" :available]}
                    "dh" #{[#inst "2021-01-01T10" :available]
                           [#inst "2021-01-01T11" :available]}
                    "berk" #{[#inst "2021-01-01T10" :available]
                             [#inst "2021-01-01T11" :available]}
                    "james" #{[#inst "2021-01-01T10" :available]
                              [#inst "2021-01-01T11" :available]}}}]
      (is (< (ps/schedule-score
              (assoc context :schedule
                     [{:guest-ids #{"raf" "dh"}
                       :at #inst "2021-01-01T10"}
                      {:guest-ids #{"raf" "berk"}
                       :at #inst "2021-01-01T11"}
                      {:guest-ids #{"james" "berk"}
                       :at #inst "2021-01-01T10"}
                      {:guest-ids #{"james" "dh"}
                       :at #inst "2021-01-01T11"}]))
             (ps/schedule-score
              (assoc context :schedule
                     [{:guest-ids #{"raf" "dh"}
                       :at #inst "2021-01-01T10"}
                      {:guest-ids #{"raf" "dh"}
                       :at #inst "2021-01-01T11"}
                      {:guest-ids #{"james" "berk"}
                       :at #inst "2021-01-01T10"}
                      {:guest-ids #{"james" "berk"}
                       :at #inst "2021-01-01T11"}])))))))

(deftest optimize-schedule
  (testing "basic"
    (is (= (set [{:guest-ids #{"raf" "dh"}
                  :at #inst "2021-01-01T10"}
                 {:guest-ids #{"raf" "berk"}
                  :at #inst "2021-01-01T11"}
                 {:guest-ids #{"berk" "dh"}
                  :at #inst "2021-01-01T12"}])
           (->> {:availabilities
                 {"raf" #{[#inst "2021-01-01T10" :available]
                          [#inst "2021-01-01T11" :available]}
                  "dh" #{[#inst "2021-01-01T10" :available]
                         [#inst "2021-01-01T11" :available]
                         [#inst "2021-01-01T12" :available]}
                  "berk" #{[#inst "2021-01-01T11" :available]
                           [#inst "2021-01-01T12" :available]}}}
                (ps/generate-initial-schedule 1)
                ps/optimize-schedule
                :schedule
                set))))

  (testing "prefer scheduling during preferred times"
   (is (= #{{:guest-ids #{"raf" "dh"}
             :at #inst "2021-01-01T10"}
            {:guest-ids #{"raf" "berk"}
             :at #inst "2021-01-01T11"}
            {:guest-ids #{"berk" "dh"}
             :at #inst "2021-01-01T12"}}
          (->> {:availabilities
                {"raf" #{[#inst "2021-01-01T10" :preferred]
                         [#inst "2021-01-01T11" :preferred]
                         [#inst "2021-01-01T12" :available]}

                 "dh" #{[#inst "2021-01-01T10" :preferred]
                        [#inst "2021-01-01T11" :available]
                        [#inst "2021-01-01T12" :preferred]}
                 "berk" #{[#inst "2021-01-01T10" :available]
                          [#inst "2021-01-01T11" :preferred]
                          [#inst "2021-01-01T12" :preferred]}}}
               (ps/generate-initial-schedule 1)
               ps/optimize-schedule
               :schedule
               set))))

  (testing "prefer evenly spreading events amongst users"
    (let [schedule (->> {:availabilities
                         {"raf" #{[#inst "2021-01-01T10" :available]
                                  [#inst "2021-01-02T10" :available]
                                  [#inst "2021-01-04T10" :available]}
                          "dh" #{[#inst "2021-01-01T10" :available]
                                 [#inst "2021-01-02T10" :available]
                                 [#inst "2021-01-04T10" :available]}
                          "berk" #{[#inst "2021-01-01T10" :available]
                                   [#inst "2021-01-02T10" :available]
                                   [#inst "2021-01-04T10" :available]}}}
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
    (is (= {"raf" #{[#inst "2021-01-01T10" :available]
                    [#inst "2021-01-02T10" :available]
                    [#inst "2021-01-04T10" :preferred]}
            "dh" #{[#inst "2021-01-01T10" :preferred]
                   [#inst "2021-01-02T10" :preferred]
                   [#inst "2021-01-04T10" :preferred]}
            "berk" #{[#inst "2021-01-01T10" :preferred]
                     [#inst "2021-01-02T10" :available]
                     [#inst "2021-01-04T10" :available]}}
           (ps.import/update-available-to-preferred
            {"raf" #{[#inst "2021-01-01T10" :available]
                     [#inst "2021-01-02T10" :available]
                     [#inst "2021-01-04T10" :preferred]}
             "dh" #{[#inst "2021-01-01T10" :available]
                    [#inst "2021-01-02T10" :available]
                    [#inst "2021-01-04T10" :available]}
             "berk" #{[#inst "2021-01-01T10" :preferred]
                      [#inst "2021-01-02T10" :available]
                      [#inst "2021-01-04T10" :available]}}))))

  (testing "all users have no preferred time"
    (is (= {"raf" #{[#inst "2021-01-01T10" :preferred]
                    [#inst "2021-01-02T10" :preferred]
                    [#inst "2021-01-04T10" :preferred]}
            "dh" #{[#inst "2021-01-01T10" :preferred]
                   [#inst "2021-01-02T10" :preferred]
                   [#inst "2021-01-04T10" :preferred]}
            "berk" #{[#inst "2021-01-01T10" :preferred]
                     [#inst "2021-01-02T10" :preferred]
                     [#inst "2021-01-04T10" :preferred]}}
           (ps.import/update-available-to-preferred
            {"raf" #{[#inst "2021-01-01T10" :available]
                     [#inst "2021-01-02T10" :available]
                     [#inst "2021-01-04T10" :available]}
             "dh" #{[#inst "2021-01-01T10" :available]
                    [#inst "2021-01-02T10" :available]
                    [#inst "2021-01-04T10" :available]}
             "berk" #{[#inst "2021-01-01T10" :available]
                      [#inst "2021-01-02T10" :available]
                      [#inst "2021-01-04T10" :available]}})))))

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
          (->> {:availabilities {"alice" #{[#inst "2021-01-01T10" :available]}
                                 "bob" #{[#inst "2021-01-01T11" :available]}}
                :times-to-pair 1}
               ps/schedule
               :schedule
               set))))

  (testing "when 3 users with limited availability, pairs 2 of them, and not 3rd"
    (is (= 1
           (->> {:max-events-per-day {"alice" 1
                                      "bob" 1
                                      "cathy" 1}
                 :timezones {"alice" "America/Toronto"
                             "bob" "America/Toronto"
                             "cathy" "America/Toronto"}
                 :availabilities {"alice" #{[#inst "2021-01-01T10" :available]}
                                  "bob" #{[#inst "2021-01-01T10" :available]}
                                  "cathy" #{[#inst "2021-01-01T10" :available]}}
                 :times-to-pair 1}
                ps/schedule
                :schedule
                set
                count))))

  (testing "does not schedule more than per-week-limit"
    (is (= 1
           (->> {:max-events-per-week {"alice" 1}
                 :availabilities {"alice" #{[#inst "2021-01-01T10" :available]
                                            [#inst "2021-01-02T10" :available]}
                                  "bob" #{[#inst "2021-01-01T10" :available]}
                                  "cathy" #{[#inst "2021-01-02T10" :available]}}
                 :times-to-pair 1}
                ps/schedule
                :schedule
                set
                count))))

  (testing "handles timezones"
    ;; timezones are only relevant for figuring out max-events-per-day
    (is (= 1
           (->> {:max-events-per-day {"alice" 1
                                      "bob" 1}
                 :timezones {"alice" "UTC"
                             "bob" "UTC"}
                 :availabilities {"alice" #{[#inst "2021-01-01T00" :available]
                                            [#inst "2021-01-01T20" :available]}
                                  "bob" #{[#inst "2021-01-01T00" :available]
                                          [#inst "2021-01-01T20" :available]}}
                 :times-to-pair 2}
                ps/schedule
                :schedule
                count)))
    (is (= 2
           (->> {:max-events-per-day {"alice" 1
                                      "bob" 1}
                 :timezones {"alice" "America/Toronto"
                             "bob" "America/Toronto"}
                 ;; same as previous, but in Toronto, these are on seperate days!
                 :availabilities {"alice" #{[#inst "2021-01-01T00" :available]
                                            [#inst "2021-01-01T20" :available]}
                                  "bob" #{[#inst "2021-01-01T00" :available]
                                          [#inst "2021-01-01T20" :available]}}
                 :times-to-pair 2}
                ps/schedule
                :schedule
                count))))

  (testing "takes topics into consideration"
    (is (= 99
           (ps/individual-score
              "raf"
              {:schedule
               [{:guest-ids #{"raf" "dh"}
                 :at #inst "2021-01-03T15"}]
               :topics {"raf" #{3}
                        "dh" #{1 2}}
               :availabilities
               {"raf" #{[#inst "2021-01-03T15" :preferred]}}})))
    (is (= #{#{{:guest-ids #{"alice" "bob"}
                :at #inst "2021-01-01T10"}
               {:guest-ids #{"cathy" "donald"}
                :at #inst "2021-01-01T10"}}}
          (set (repeatedly 10
                (fn []
                  (->> {:availabilities {"alice" #{[#inst "2021-01-01T10" :available]}
                                         "bob" #{[#inst "2021-01-01T10" :available]}
                                         "cathy" #{[#inst "2021-01-01T10" :available]}
                                         "donald" #{[#inst "2021-01-01T10" :available]}}
                        :topics {"alice" #{"a" "b"}
                                 "bob" #{"a" "z"}
                                 "cathy" #{"e" "f"}
                                 "donald" #{"e" "g"}}
                        :times-to-pair 1}
                       ps/schedule
                       :schedule
                       set))))))))

(ns mycc.pairwithme.test.slots
  (:require
    [clojure.test :refer [deftest is testing]]
    [mycc.pairwithme.slots :as slots])
  (:import
    (java.time Instant ZoneId)
    (java.util Date)))

(defn ->date [s]
  (Date/from (Instant/parse s)))

(deftest availability-at
  (let [user {:user/time-zone "America/Toronto"
              :user/availability {[:thursday 19] :available}}]
    (testing "converts to user's zone (EST, UTC-5)"
      (is (= :available (slots/availability-at user (->date "2026-11-13T00:00:00Z"))))
      (is (nil? (slots/availability-at user (->date "2026-11-12T19:00:00Z")))))
    (testing "DST (EDT, UTC-4)"
      (is (= :available (slots/availability-at user (->date "2026-10-29T23:00:00Z")))))))

(deftest candidate-slots
  (let [now (->date "2026-11-04T10:30:00Z")
        candidates (slots/candidate-slots now (ZoneId/of "UTC"))]
    (testing "starts 12h from now, rounded up to the hour"
      (is (= (->date "2026-11-04T23:00:00Z") (first candidates))))
    (testing "ends before 7 days from now"
      (is (= (->date "2026-11-11T10:00:00Z") (last candidates))))
    (testing "on the hour, exactly"
      (is (= (->date "2026-11-04T22:00:00Z")
             (first (slots/candidate-slots (->date "2026-11-04T10:00:00Z") (ZoneId/of "UTC"))))))
    (testing "aligned to whole hours in a half-hour zone"
      (is (= (->date "2026-11-04T22:30:00Z")
             (first (slots/candidate-slots now (ZoneId/of "Asia/Kolkata"))))))))

(deftest listed?
  (is (slots/listed? {:user/pairwithme-show-profile? true
                      :user/availability {[:monday 9] :available}}))
  (is (not (slots/listed? {:user/pairwithme-show-profile? true
                           :user/availability {[:monday 9] nil}})))
  (is (not (slots/listed? {:user/pairwithme-show-profile? false
                           :user/availability {[:monday 9] :available}})))
  (is (not (slots/listed? {:user/availability {[:monday 9] :available}}))))

(deftest valid-slot?-and-overlap
  (let [now (->date "2026-11-04T10:00:00Z")
        proposer {:user/time-zone "Europe/Berlin"
                  :user/availability {[:thursday 10] :available}}
        recipient {:user/time-zone "UTC"
                   :user/availability {[:thursday 9] :preferred
                                       [:thursday 15] :available}}]
    (is (slots/valid-slot? {:now now
                            :proposer proposer
                            :recipient recipient
                            :slot (->date "2026-11-05T09:00:00Z")}))
    (testing "recipient not available"
      (is (not (slots/valid-slot? {:now now
                                   :proposer proposer
                                   :recipient recipient
                                   :slot (->date "2026-11-05T10:00:00Z")}))))
    (testing "less than 12h from now"
      (is (not (slots/valid-slot? {:now (->date "2026-11-05T00:00:00Z")
                                   :proposer proposer
                                   :recipient recipient
                                   :slot (->date "2026-11-05T09:00:00Z")}))))
    (testing "overlap: only Thu 9 UTC is shared"
      (is (= 1 (slots/overlap-count {:now now
                                     :viewer proposer
                                     :other recipient}))))))

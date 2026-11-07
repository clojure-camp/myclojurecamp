(ns mycc.pairwithme.test.sessions
  (:require
    [clojure.test :refer [deftest is testing]]
    [mycc.pairwithme.sessions :as sessions])
  (:import
    (java.time Instant)
    (java.util Date)))

(defn ->date [s]
  (Date/from (Instant/parse s)))

(def alice-id (random-uuid))
(def bob-id (random-uuid))
(def now (->date "2026-11-04T10:00:00Z"))
(def slot-a (->date "2026-11-05T09:00:00Z"))
(def slot-b (->date "2026-11-06T09:00:00Z"))

(def session
  (sessions/new-session {:initiator-id bob-id
                         :recipient-id alice-id
                         :slots [slot-a slot-b]
                         :now now}))

(def first-proposal-id
  (:proposal/id (sessions/active-proposal session)))

(deftest roles
  (is (= alice-id (sessions/recipient-id session)))
  (is (= :respond (sessions/status-for session alice-id)))
  (is (= :waiting (sessions/status-for session bob-id)))
  (let [countered (sessions/add-proposal session {:from-user-id alice-id
                                                  :slots [slot-b]
                                                  :now now})]
    (is (= bob-id (sessions/recipient-id countered)))
    (is (= :respond (sessions/status-for countered bob-id)))))

(deftest accept-error
  (let [params {:session session
                :user-id alice-id
                :proposal-id first-proposal-id
                :slot-index 1
                :now now}]
    (is (nil? (sessions/accept-error params)))
    (testing "wrong user"
      (is (= :wrong-user (sessions/accept-error (assoc params :user-id bob-id)))))
    (testing "missing slot"
      (is (= :not-found (sessions/accept-error (assoc params :slot-index 5)))))
    (testing "past"
      (is (= :past (sessions/accept-error (assoc params :now (->date "2026-11-07T00:00:00Z"))))))
    (testing "already accepted"
      (is (= :already-accepted
             (sessions/accept-error
               (assoc params :session (sessions/accept session {:user-id alice-id
                                                                :slot slot-a
                                                                :now now}))))))
    (testing "replaced"
      (is (= :replaced
             (sessions/accept-error
               (assoc params
                 :user-id bob-id
                 :session (sessions/add-proposal session {:from-user-id alice-id
                                                          :slots [slot-b]
                                                          :now now}))))))))

(deftest upcoming?
  (is (sessions/upcoming? session now))
  (is (sessions/upcoming? session (->date "2026-11-06T09:30:00Z")))
  (is (not (sessions/upcoming? session (->date "2026-11-06T10:00:00Z"))))
  (let [accepted (sessions/accept session {:user-id alice-id
                                           :slot slot-a
                                           :now now})]
    (is (not (sessions/upcoming? accepted (->date "2026-11-05T10:00:00Z"))))))

(deftest message-ids
  (is (= (str "<pairwithme-" (:pairwithme-session/id session) "-" first-proposal-id "@clojure.camp>")
         (:proposal/email-message-id (sessions/active-proposal session)))))

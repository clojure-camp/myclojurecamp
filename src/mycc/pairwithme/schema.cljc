(ns mycc.pairwithme.schema
  (:require
    [mycc.base.schema :refer [NonBlankString]]))

(def max-slots-per-proposal 3)

(def Proposal
  [:map
   [:proposal/id :uuid]
   [:proposal/from-user-id :uuid]
   [:proposal/created-at inst?]
   ;; UTC start times, 1h each
   [:proposal/slots [:vector {:min 1 :max max-slots-per-proposal} inst?]]
   [:proposal/email-message-id NonBlankString]])

(def PairwithmeSession
  [:map
   [:pairwithme-session/id :uuid]
   [:pairwithme-session/initiator-id :uuid]
   [:pairwithme-session/recipient-id :uuid]
   [:pairwithme-session/created-at inst?]
   ;; oldest first; last one is the active proposal
   [:pairwithme-session/proposals [:vector {:min 1} Proposal]]
   [:pairwithme-session/accepted-slot [:maybe inst?]]
   [:pairwithme-session/accepted-by-user-id [:maybe :uuid]]
   [:pairwithme-session/accepted-at [:maybe inst?]]])

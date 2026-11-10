(ns mycc.pairwithme.schema
  (:require
    [modulo.api :as mod]
    [mycc.base.schema :refer [NonBlankString]]))

(def max-slots-per-proposal 3)

(mod/register-entity!
  :entity/proposal
  [[:proposal/id :uuid]
   [:proposal/from-user-id :user/id]
   [:proposal/created-at inst?]
   ;; UTC start times, 1h each
   [:proposal/slots [:vector {:min 1 :max max-slots-per-proposal} inst?]]
   [:proposal/email-message-id NonBlankString]])

(mod/register-entity!
  :entity/pairwithme-session
  [[:pairwithme-session/id :uuid]
   [:pairwithme-session/initiator-id :user/id]
   [:pairwithme-session/recipient-id :user/id]
   [:pairwithme-session/created-at inst?]
   ;; oldest first; last one is the active proposal
   [:pairwithme-session/proposals [:vector {:min 1} :entity/proposal]]
   [:pairwithme-session/accepted-slot [:maybe inst?]]
   [:pairwithme-session/accepted-by-user-id [:maybe :user/id]]
   [:pairwithme-session/accepted-at [:maybe inst?]]])

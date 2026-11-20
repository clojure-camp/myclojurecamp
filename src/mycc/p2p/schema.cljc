(ns mycc.p2p.schema
  (:require
    [modulo.api :as mod]
    [mycc.base.schema]))

(mod/register-entity!
  :entity/event
  [[:event/id :uuid]
   [:event/at inst?]
   [:event/guest-ids [:set :user/id]]])

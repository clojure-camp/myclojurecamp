(ns mycc.profile.schema
  (:require
    [modulo.api :as mod]
    [mycc.base.schema :as base.schema]))

(mod/register-entity!
  :entity/topic
  [[:topic/id :uuid]
   [:topic/name base.schema/NonBlankString]
   [:topic/user-count {:optional true} :int]])

(ns mycc.profile.core
  (:require
    #?@(:clj
         [[mycc.profile.cqrs]]
         :cljs
         [[mycc.profile.state]
          [mycc.profile.ui]])))

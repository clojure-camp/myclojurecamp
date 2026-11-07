(ns mycc.pairwithme.core
  (:require
    #?@(:clj
         [[mycc.pairwithme.cqrs]
          [mycc.pairwithme.announcement-job]]
         :cljs
         [[mycc.pairwithme.state]
          [mycc.pairwithme.ui]])))

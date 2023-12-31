(ns mycc.p2p.core
  (:require
    #?@(:clj
         [[mycc.p2p.cqrs]
          [mycc.p2p.opt-in-email-job]
          [mycc.p2p.match-email-job]]
         :cljs
         [[mycc.p2p.ui]
          [mycc.p2p.state]])))


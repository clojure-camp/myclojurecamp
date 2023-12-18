(ns mycc.base.core
  (:require
    [bloom.omni.core :as omni]
    [mycc.base.cqrs] ;; for side-effects
    [mycc.p2p.opt-in-email-job :as p2p.opt-in-email]
    [mycc.p2p.match-email-job :as p2p.match-email]
    [mycc.base.omni-config :refer [omni-config]]))

(defn set-default-exception-handler
  []
  (Thread/setDefaultUncaughtExceptionHandler
   (reify Thread$UncaughtExceptionHandler
     (uncaughtException [_ thread ex]
       (println ex "Uncaught exception " (.getName thread))))))

(defn start! []
  (set-default-exception-handler)
  (omni/start! omni/system (omni-config))
  (p2p.opt-in-email/schedule-email-job!)
  (p2p.match-email/schedule-email-job!)
  ;; return nil, b/c output for jobs stalls some REPLs
  nil)

(defn stop! []
  (omni/stop!))

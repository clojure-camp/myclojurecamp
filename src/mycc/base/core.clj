(ns mycc.base.core
  (:require
    [bloom.omni.core :as omni]
    [mycc.base.cqrs] ;; for side-effects
    [mycc.base.jobs :as jobs]
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
  (jobs/initialize!))

(defn stop! []
  (omni/stop!))

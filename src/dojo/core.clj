(ns dojo.core
  (:gen-class)
  (:require
    [bloom.omni.core :as omni]
    [dojo.jobs.opt-in-email :as jobs.opt-in-email]
    [dojo.jobs.match-email :as jobs.match-email]
    [dojo.config :as config]
    [dojo.omni-config :refer [omni-config]]
    [dojo.seed :as seed]))

(defn set-default-exception-handler
  []
  (Thread/setDefaultUncaughtExceptionHandler
   (reify Thread$UncaughtExceptionHandler
     (uncaughtException [_ thread ex]
       (println ex "Uncaught exception " (.getName thread))))))

(defn start! []
  (set-default-exception-handler)
  (omni/start! omni/system (omni-config))
  (jobs.opt-in-email/schedule-email-job!)
  (jobs.match-email/schedule-email-job!))

(defn stop! []
  (omni/stop!))

(defn -main []
  (start!))

#_(start!)
#_(seed/seed!)

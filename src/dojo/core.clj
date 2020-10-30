(ns dojo.core
  (:gen-class)
  (:require
    [bloom.omni.core :as omni]
    [dojo.jobs.opt-in-email :as jobs.opt-in-email]
    [dojo.omni-config :refer [omni-config]]))

(defn start! []
  #_(seed/seed!)
  (omni/start! omni/system omni-config)
  (jobs.opt-in-email/schedule-email-job!))

(defn stop! []
  (omni/stop!))

(defn -main []
  (start!))



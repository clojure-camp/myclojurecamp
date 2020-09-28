(ns dojo.core
  (:gen-class)
  (:require
    [bloom.omni.core :as omni]
    [dojo.email :as email]
    [dojo.omni-config :refer [omni-config]]))

(defn start! []
  #_(seed/seed!)
  (omni/start! omni/system omni-config)
  (email/schedule-email-job!))

(defn stop! []
  (omni/stop!))

(defn -main []
  (start!))



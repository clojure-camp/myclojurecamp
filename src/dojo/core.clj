(ns dojo.core
  (:gen-class)
  (:require
    [dojo.email :as email]))

(defn -main []
  (email/schedule-email-job!))



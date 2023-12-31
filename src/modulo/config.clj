(ns modulo.config
  (:require
    [clojure.java.io :as io]
    [bloom.commons.config :as config]))

(defonce config-config
  (atom
    {:schema [:map]
     :default {}}))

(defn initialize!
  [schema default]
  (reset! config-config {:schema schema
                         :default default}))

(defn generate! []
  (spit "config.edn"
        (:default @config-config)))

(def state
  (delay
    (when (not (.exists (io/file "config.edn")))
      (println "No config.edn detected, creating a default file.")
      (generate!))
    (config/read "config.edn" (:schema @config-config))))

#_(deref config)

(defn config [korks]
  (if (vector? korks)
    (get-in @state korks)
    (get @state korks)))

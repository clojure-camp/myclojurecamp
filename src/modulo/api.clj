(ns modulo.api
  (:require
    [modulo.cqrs-registry :as cqrs]
    [modulo.jobs :as jobs]
    [modulo.config :as config]
    [modulo.system :as system]))

(defn register-cqrs! [& args]
  (apply cqrs/register-cqrs! args))

(defn config [& args]
  (apply config/config args))

(defn register-job! [& args]
  (apply jobs/register-job! args))

(defn start! [& args]
  (apply system/start! args))

(defn stop! [& args]
  (apply system/stop! args))

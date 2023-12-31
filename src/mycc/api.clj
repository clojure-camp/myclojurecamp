(ns mycc.api
  (:require
    [mycc.base.cqrs-registry :as cqrs]
    [mycc.base.config :as config]
    [mycc.base.jobs :as jobs]))

(defn register-cqrs! [& args]
  (apply cqrs/register-cqrs! args))

(defn config [korks]
  (if (vector? korks)
    (get-in @config/config korks)
    (get @config/config korks)))

(defn register-job! [& args]
  (apply jobs/register-job! args))

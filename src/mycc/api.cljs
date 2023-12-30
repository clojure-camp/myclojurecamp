(ns mycc.api
  (:require
    [re-frame.core :as re-frame]
    [mycc.base.client.pages :as pages]))

(defn register-page! [& args]
  (apply pages/register-page! args))

;; re-frame

(defn reg-sub [& args]
  (apply re-frame/reg-sub args))

(defn reg-event-fx [& args]
  (apply re-frame/reg-event-fx args))

(defn dispatch [& args]
  (apply re-frame/dispatch args))

(defn subscribe [& args]
  (apply re-frame/subscribe args))

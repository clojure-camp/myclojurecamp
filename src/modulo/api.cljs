(ns modulo.api
  (:require
    [re-frame.core :as re-frame]
    [modulo.client.pages :as pages]
    [modulo.client.core :as core]))

;; root

(defn initialize! [root-view]
  (reset! core/root-view root-view)
  (core/init))

;; pages

(defn register-page! [& args]
  (apply pages/register-page! args))

(defn pages []
  (pages/all))

(defn path-for [& args]
  (apply pages/path-for args))

(defn active? [& args]
  (apply pages/active? args))

(defn current-page-view [& args]
  (apply pages/current-page-view args))

;; re-frame

(defn reg-sub [& args]
  (apply re-frame/reg-sub args))

(defn reg-event-fx [& args]
  (apply re-frame/reg-event-fx args))

(defn dispatch [& args]
  (apply re-frame/dispatch args))

(defn subscribe [& args]
  (apply re-frame/subscribe args))

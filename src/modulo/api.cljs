(ns modulo.api
  (:require
    [modulo.client.pages :as pages]
    [modulo.client.core :as core]
    [modulo.schema :as schema]
    [modulo.client.tada :as tada]))

;; root

(defn initialize! [root-view]
  (reset! core/root-view root-view)
  (core/init))

;; tada

(defn tada! [& args]
  (apply tada/tada! args))

(defn tada-atom [& args]
  (apply tada/tada-atom args))

(defn refresh! [& args]
  (apply tada/refresh! args))

(defn saved? []
  (tada/saved?))

;; schema

(defn register-entity! [& args]
  (apply schema/register-entity! args))

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

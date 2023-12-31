(ns mycc.base.client.pages
  (:require
    [reagent.core :as r]
    [bloom.commons.pages :as bloom.pages]))

(defn all
  "A reagent atom of registered pages"
  []
  bloom.pages/pages)

(defn register-page! [page]
  (bloom.pages/register-page! page))

(defn path-for [& args]
  (apply bloom.pages/path-for args))

(defn current-page-view []
  (bloom.pages/current-page-view))

(defn active? [& args]
  (apply bloom.pages/active? args))

(defn initialize! []
  (bloom.pages/initialize! []))

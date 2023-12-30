(ns mycc.common.mixins
  (:require
    [mycc.common.colors :as colors]))

(defn text-input []
  [:&
   {:font-size "1.1em"
    :padding "0.25em"
    :font-weight 300
    :font-family "Roboto, sans-serif"}])

(defn button []
  [:&
   {:background-color colors/clojure-blue
    :border "none"
    :color "white"
    :padding "0.25rem 0.5rem"
    :cursor "pointer"
    :font-size "1em"
    :box-sizing "border-box"
    :border-radius "0.2rem"}

   [:&:hover
    {:background-color colors/clojure-blue-darker}]])



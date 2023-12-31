(ns mycc.common.mixins
  (:require
    [garden.color :refer [darken]]
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

(defn field []
  [:&
   {:width "100%"
    :padding "1rem 0"
    :border-bottom [["1px" "solid" "#ccc"]]
    :font-weight 300}

   [:h1
    {:font-size "1.1rem"
     :font-weight 700
     :margin [[0 0 "1rem" 0]]
     :display "flex"
     :align-items "center"
     :gap "0.4em"}]

   [:.info
    {:position "relative"}

    [:svg
     {:width "0.85em"
      :height "0.85em"
      :color colors/light-text}]

    [:.popover
     {:display "none"
      :border [["1px" "solid" "#ccc"]]
      :position "absolute"
      :background "white"
      :padding "0.5em"
      :width "100vw"
      :left "1em"
      :top 0
      :max-width "30em"
      :font-weight 300
      :font-size "1rem"}]

    [:&:hover
     [:.popover
      {:display "block"}]]]])

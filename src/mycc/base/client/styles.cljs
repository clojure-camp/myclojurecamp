(ns mycc.base.client.styles
  (:require
    [garden.stylesheet :refer [at-import at-keyframes]]
    [garden.color :refer [lighten]]
    [mycc.common.colors :as colors]
    [mycc.common.mixins :as mixins]))

(def styles
  [(at-import "https://fonts.googleapis.com/css2?family=Roboto:wght@300;500&family=Walter+Turncoat&display=swap")

   (at-keyframes "fade-out"
     ["0%" {:opacity 1}]
     ["100%" {:opacity 0}])

   (at-keyframes "spin"
     ["0%" {:transform "rotate(0deg)"}]
     ["100%" {:transform "rotate(359deg)"}])

   [:.ajax-status
    {:position "fixed"
     :top "1em"
     :left "3em"
     :right 0}

    [:>svg
     {:width "1em"
      :height "1em"}]

    [:&.loading>svg
     {:animation "spin 1s infinite linear"
      :color "white"}]

    [:&.normal>svg
     {:animation "fade-out 1s forwards ease-in-out"
      :color "white"}]]

   [:body
    {:font-family "Roboto, sans-serif"
     :margin 0}]

   [:#app

    [:.login
     {:min-height "100vh"
      :display "flex"
      :justify-content "center"
      :align-items "center"
      :flex-direction "column"
      :height "100%"
      ;; for star-field:
      :z-index 1
      :position "relative"
      :background colors/clojure-camp-blue
      :color "white"}

     [:>.star-field
      {:position "absolute"
       :z-index -1
       :top 0
       :right 0
       :bottom 0
       :left 0
       :background colors/clojure-camp-blue}]

     [:>img.logomark
      {:max-height "30vh"
       :max-width "60vw"}]

     [:>h1
      {:margin [[0 0 "2em" 0]]}

      [:>img.logotype
       {:max-width "50vw"
        :max-height "10vh"}]]

     [:>form
      {:display "flex"
       :flex-direction "column"
       :background colors/clojure-camp-blue
       :padding "0.5em"
       :min-width "20em"
       :max-width "80vw"}

      [:>label>input
       :>button
       {:display "block"
        :height "2em"
        :font-size "1em"
        :margin "0.5em 0"
        :padding "0.25em"
        :border "none"
        :box-sizing "border-box"
        :border-radius "0.2rem"}]

      [:>label>input
       {:width "100%"}]

      [:>button
       {:background-color "white"
        :color colors/clojure-camp-blue
        :font-family "Roboto"
        :padding "0 0.5rem"
        :cursor "pointer"}]]]

    [:.main

     [:>.header
      {:background colors/clojure-camp-blue
       :display "flex"
       :justify-content "space-between"
       :align-items "center"
       :color "white"
       :padding "0.5em"
       :box-sizing "border-box"
       :width "100vw"}

      [:>img.logomark
       {:height "2em"
        ;; to balance out log-out button
        :padding-right "3em"}]

      [:>img.logotype
       {:height "1.5em"}]

      [:>.log-out
       (mixins/button)]]

     [:>.nav
      {:background (lighten colors/clojure-camp-blue 20)}

      [:>.items
       {:max-width "40em"
        :display "flex"
        :justify-content "center"
        :margin "0 auto"
        :gap "1em"}

       [:>a
        {:padding "0.75em"
         :text-transform "uppercase"
         :letter-spacing "0.1em"
         :color "white"
         :text-decoration "none"}

        {:opacity 0.5}

        [:&.active
         {:opacity 1
          :border-bottom "2px solid white"}]]]]

     [:>.content
      {:padding "2em"
       :display "flex"
       :flex-direction "column"
       :align-items "center"
       :max-width "40em"
       :margin "0 auto"
       :background "white"}]]]])

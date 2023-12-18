(ns mycc.client.styles
  (:require
    [garden.stylesheet :refer [at-import at-keyframes]]
    [garden.color :refer [darken]]))

(def accent-light "#4576bf")
(def accent-dark "#2b468d")

(def gray "#f3f3f3")
(def clojure-green "#5FAD31")
(def clojure-blue "#567ED2")
(def clojure-blue-darker "#396CD5")
(def clojure-camp-blue "#181742")
(def light-text "#bbb")

(defn text-input []
  [:&
   {:font-size "1.1em"
    :padding "0.25em"
    :font-weight 300
    :font-family "Roboto, sans-serif"}])

(defn button []
  [:&
   {:background-color clojure-blue
    :border "none"
    :color "white"
    :padding "0.25rem 0.5rem"
    :cursor "pointer"
    :font-size "1em"
    :box-sizing "border-box"
    :border-radius "0.2rem"}

   [:&:hover
    {:background-color clojure-blue-darker}]])

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
      :background clojure-camp-blue
      :color "white"}

     [:>.star-field
      {:position "absolute"
       :z-index -1
       :top 0
       :right 0
       :bottom 0
       :left 0
       :background clojure-camp-blue}]

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
       :background clojure-camp-blue
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
        :color clojure-camp-blue
        :font-family "Roboto"
        :padding "0 0.5rem"
        :cursor "pointer"}]]]

    [:.main

     [:>.header
      {:background clojure-camp-blue
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
       (button)]]

     [:>.content
      {:padding "2em"
       :display "flex"
       :flex-direction "column"
       :align-items "center"
       :max-width "40em"
       :margin "0 auto"
       :background "white"}

      [:section.field
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
          :color light-text}]

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
          {:display "block"}]]]

       ;; specific sections

       [:&.name
        :&.max-pair-day
        :&.max-pair-week
        "" ;; ¯\_(ツ)_/¯
        [:input
         (text-input)]]

       [:&.topics
        [:>.warning
         {:color "red"
          :background "#ffe9e9"
          :padding "0.25em"
          :border-radius "0.25em"
          :border "1px solid #ffc4c4"}

         [:>svg
          {:width "1em"
           :margin-right "0.25em"}]]

        [:>.topics
         {:columns "3"}

         [:>.topic
          {:display "block"
           :margin-bottom "0.5rem"
           :cursor "pointer"
           :vertical-align "center"
           :white-space "nowrap"}

          [:>.count
           {:color "#ccc"}]]

         [:>button
          (button)]]]

       [:&.time-zone
        [:button
         (button)]]

       [:&.role
        :&.opt-in
        :&.subscribed
        "" ;; ¯\_(ツ)_/¯
        [:.choices
         {:display "flex"
          :gap "1em"}

         [:label
          {:cursor "pointer"
           :font-size "1.1em"}

          [:.label
           {:margin-left "0.1em"}]]]]

       [:&.availability
        [:>table
         {:border-spacing 0
          :width "100%"}

         [:th.day
          {:font-weight "normal"
           :padding-bottom "1rem"}

          [:>.day-of-week]

          [:>.date
           {:color "#aaa"}]]

         [:td.hour
          {:text-align "right"
           :padding-right "1rem"
           :vertical-align "top"
           :transform "translateY(-0.5rem)"}]

         [:td
          {:padding 0}

          [:>button
           {:width "100%"
            :border "none"
            :cursor "pointer"
            :padding "1em"
            :height "6em"
            :display "flex"
            :justify-content "center"
            :align-items "center"}

           [:&.empty
            {:background "#fff"
             :color "#aaa"}

            [:>.wrapper
             {:border "1px dashed #ccc"
              :height "4em"
              :width "100%"
              :line-height "4em"}]

            [:&:hover
             {:background (darken "#fff" 10)}]]

           [:&.preferred
            {:background accent-dark
             :color "white"}

            [:&:hover
             {:background (darken accent-dark 10)}]]

           [:&.available
            {:background accent-light
             :color "white"}

            [:&:hover
             {:background (darken accent-light 10)}]]]]]]]

      [:>.unsubscribe
       (button)]]

     [:table.events
      [:>tbody
       [:>tr
        [:&.past
         {:opacity "0.5"}]

        [:>th
         {:vertical-align "top"
          :text-align "right"
          :padding "0.5em"}]

        [:>td
         {:padding "0.5em"}

         [:>.actions
          {:display "flex"
           :gap "0.5em"
           :align-items "center"}

          [:>.link
           [:>svg
            {:width "1em"}]]

          [:>button.flag
           {:cursor "pointer"
            :border "none"
            :background "none"
            :padding 0
            :color "#AAA"}

           [:&:hover
            {:transform "scale(1.25)"}]

           [:&.flagged
            {:color "red"}]

           [:>svg
            {:width "1em"}]]]]]]]]]])

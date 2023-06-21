(ns dojo.client.styles
  (:require
    [garden.stylesheet :refer [at-import at-keyframes]]
    [garden.color :refer [darken]]))

(def accent-light "#45c077")
(def accent-dark "#2b8d53")
(def gray "#f3f3f3")
(def clojure-green "#5FAD31")
(def clojure-blue "#567ED2")
(def clojure-blue-darker "#396CD5")
(def clojure-camp-blue "#181742")

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
          {:width "1em"}]]]]]]]

   [:.ajax-status
    {:position "fixed"
     :top "1em"
     :left 0
     :right 0
     :display "flex"
     :justify-content "center"}

    [:>svg
     {:width "1em"
      :height "1em"}]

    [:&.loading>svg
     {:animation "spin 1s infinite linear"
      :color "gray"}]

    [:&.normal>svg
     {:animation "fade-out 1s forwards ease-in-out"}]]

   [:.log-out
    (button)
    {:position "absolute"
     :top "0.25rem"
     :right "0.25rem"}]

   [:body
    {;;:background (str "linear-gradient(-225deg, " clojure-green "ee, " clojure-blue "ee)")
     :font-family "Roboto, sans-serif"
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
     {:display "flex"
      :flex-direction "column"
      :align-items "center"
      :padding "2rem"
      :gap "2rem"}
     {:max-width "40em"
      :margin "0 auto"
      :background "white"}

     [:>.opt-in
      {:display "flex"
       :align-items "center"
       :font-size "2rem"
       :background gray
       :border [["1px" "solid" (darken gray 10)]]
       :padding "1rem"
       :border-radius "0.5rem"
       :font-weight "bold"
       :cursor "pointer"}

      [:&:hover
       {:background (darken gray 5)}]

      [:&.active
       {:background accent-light
        :border [["1px" "solid" (darken accent-light 10)]]}

       [:&:hover
        {:background (darken accent-light 5)}]]

      [:>svg
       {:width "2rem"
        :margin-right "0.5rem"}]

      [:>input
       {:display "none"}]]

     [:>.topics-view
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

     [:>.max-limit-preferences

      [:>label
       {:display "block"
        :padding "0.5rem"}

       [:>input
        {:margin-left "0.5rem"}]]]

     [:>.time-zone
      {:display "flex"
       :gap "0.25em"
       :align-items "center"}
      [:>button
       (button)]]

     [:>table.availability
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
          {:background (darken "#fff" 5)}]]

        [:&.preferred
         {:background accent-dark
          :color "white"}

         [:&:hover
          {:background (darken accent-dark 5)}]]

        [:&.available
         {:background accent-light
          :color "white"}

         [:&:hover
          {:background (darken accent-light 5)}]]]]]

     [:>.unsubscribe
      (button)]]]])

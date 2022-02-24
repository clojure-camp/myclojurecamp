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
  [(at-import "https://fonts.googleapis.com/css2?family=Roboto:wght@300;700&display=swap")

   (at-keyframes "fade-out"
     ["0%" {:opacity 1}]
     ["100%" {:opacity 0}])

   (at-keyframes "spin"
     ["0%" {:transform "rotate(0deg)"}]
     ["100%" {:transform "rotate(359deg)"}])

   [:.events
    [:>.event
     {:display "flex"
      :gap "0.5em"
      :padding "0.5em"
      :align-items "center"}

     [:&.past
      {:opacity "0.5"}]

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
       {:width "1em"}]]]]

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
    {:background (str "linear-gradient(-225deg, " clojure-green "ee, " clojure-blue "ee)")
     :font-family "Roboto, sans-serif"
     :margin 0}]

   [:#app
    {:max-width "40em"
     :min-height "100vh"
     :margin "0 auto"
     :background "white"}

    [:.login
     {:display "flex"
      :justify-content "center"
      :align-items "center"
      :flex-direction "column"
      :height "100%"}

     [:>img
      {:max-width "15vw"}]

     [:>form>label>input
      :>form>button
      {:height "2em"
       :font-size "1em"
       :padding "0.25em"
       :border "1px solid black"
       :box-sizing "border-box"
       :margin-left "0.5rem"
       :border-radius "0.2rem"}]

     [:>form>label>input
      {:border-color clojure-green}]

     [:>form>button
      {:background-color clojure-blue
       :border "1px solid black"
       :border-color clojure-blue
       :color "white"
       :padding "0 0.5rem"
       :cursor "pointer"}]]

    [:.main
     {:display "flex"
      :flex-direction "column"
      :align-items "center"
      :padding "2rem"
      :gap "2rem"}

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
        :padding-right "1rem"}]

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

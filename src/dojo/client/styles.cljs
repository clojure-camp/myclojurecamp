(ns dojo.client.styles
  (:require
    [garden.stylesheet :refer [at-import]]
    [garden.color :refer [darken]]))

(def accent-color "blue")

(defn button [])

(def styles
  [(at-import "https://fonts.googleapis.com/css2?family=Roboto:wght@300;700&display=swap")

   [:.log-out
    {:position "absolute"
     :top 0
     :right 0}]

   [:body
    {:background "black"
     :font-family "Roboto, sans-serif"
     :margin 0}]

   [:#app
    {:max-width "40em"
     :margin "0 auto"
     :background "white"}

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
       :background "#f3f3f3"
       :padding "1rem"
       :border "1px solid #e1e1e1"
       :border-radius "0.5rem"
       :font-weight "bold"
       :cursor "pointer"}

      [:>svg
       {:width "2rem"
        :margin-right "0.5rem"}]

      [:>input
       {:display "none"}]]

     [:>.topics-view

      [:>.topics
       {:columns "3"}

       [:>.topic
        {:display "block"
         :margin-bottom "0.5rem"
         :vertical-align "center"}

        [:>.count
         {:color "#ccc"}]]]]

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
         :height "5em"
         :border "none"
         :cursor "pointer"
         :color "#13502c"}

        [:&.empty
         {:background "#fff"}

         [:&:hover
          {:background (darken "#fff" 5)}]]

        [:&.preferred
         {:background "#2b8d53"}

         [:&:hover
          {:background (darken "#2b8d53" 5)}]]

        [:&.available
         {:background "#45c077"}

         [:&:hover
          {:background (darken "#45c077" 5)}]]]]]]]])

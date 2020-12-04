(ns dojo.client.styles)

(def accent-color "blue")

(def styles
  [[:.log-out
    {:position "absolute"
     :top 0
     :right 0}]

   [:body
    {:background "black"
     :margin 0}]

   [:#app
    {:max-width "40em"
     :margin "0 auto"
     :background "white"}]

   [:.main

    [:>table.availability
     {:border-spacing 0
      :width "100%"}

     [:td
      {:padding 0}

      [:>button
       {:width "100%"
        :height "5em"
        :border "none"
        :cursor "pointer"}

       [:&.empty
        {:background "white"}]

       [:&.preferred
        {:background "blue"}]

       [:&.available
        {:background "aqua"}]

       [:&:hover
        {:background "pink"}]

       [:&:active
        {:background "green"}]]]]]])

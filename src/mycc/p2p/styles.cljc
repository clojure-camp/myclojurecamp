(ns mycc.p2p.styles
  (:require
    [garden.color :refer [darken]]
    [mycc.common.colors :as colors]))

(def styles
  [:.page.p2p

   [:table.availability
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
       :height "3em"
       :display "flex"
       :justify-content "center"
       :align-items "center"}

      [:>.wrapper
        {:height "2em"
         :width "100%"
         :line-height "2em"}]

      [:&.empty
       {:background "#fff"
        :color "#aaa"}

       [:>.wrapper
        {:border "1px dashed #ccc"}]

       [:&:hover
        {:background (darken "#fff" 10)}]]

      [:&.preferred
       [:>.wrapper
        {:background colors/accent-dark
         :color "white"}]

       [:&:hover
        {:background (darken colors/accent-dark 10)}]]

      [:&.available
       [:>.wrapper
        {:background colors/accent-light
         :color "white"}]

       [:&:hover
        {:background (darken colors/accent-light 10)}]]]]]

   [:.topics-section

    [:>.topics
     {:columns "3"}

     [:>.topic
      {:display "block"
       :margin-bottom "0.5rem"
       :cursor "pointer"
       :vertical-align "center"
       :white-space "nowrap"}

      [:>input
       {:margin-right "0.25em"}]

      [:>.count
       {:color "#ccc"}]]]]

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
          {:width "1em"}]]]]]]]])

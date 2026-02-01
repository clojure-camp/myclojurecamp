(ns mycc.p2p.styles
  (:require
    [garden.color :refer [darken]]
    [mycc.common.colors :as colors]
    [mycc.common.availability :as availability]))

(def styles
  [:.page.p2p

   availability/styles

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

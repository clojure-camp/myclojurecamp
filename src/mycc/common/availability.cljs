(ns mycc.common.availability
  (:require
   [clojure.string :as string]
   [reagent.core :as r]
   [modulo.api :as mod]
   [mycc.common.colors :as colors]
   [mycc.p2p.util :as util]
   [mycc.common.ui :as ui]))

(def styles
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
    {:padding "0.125rem"}

    [:>button
     {:width "100%"
      :height "2rem"
      :padding 0
      :border-width "1px"
      :border-style "solid"
      :border-radius "0.25rem"
      :cursor "pointer"
      :font-size "0.75rem"
      :display "flex"
      :justify-content "center"
      :align-items "center"
      :overflow "hidden"}

     [:&.empty
      {:background "#fff"
       :border "1px dashed #ccc"
       :color "#aaa"}

      [:&:hover
       {:background "#f3f4f6"}]]

     [:&.preferred
      {:background "#93c5fd"
       :border-color "#60a5fa"
       :color colors/accent-dark}

      [:&:hover
       {:background "#60a5fa"}]]

     [:&.available
      {:background "#dbeafe"
       :border-color "#bfdbfe"
       :color colors/accent-dark}

      [:&:hover
       {:background "#bfdbfe"}]]]]])

(defn next-day-of-week
  "Calculates next date with day of week as given"
  [now target-day-of-week]
  (let [target-day-of-week ({:monday 1
                             :tuesday 2
                             :wednesday 3
                             :thursday 4
                             :friday 5
                             :saturday 6
                             :sunday 0} target-day-of-week)
        now-day-of-week (.getDay now)
        ;; must be a nice way to do this with mod
        ;; (mod (- 7 now-day-of-week) 7)
        delta-days (get (zipmap (range 0 7)
                                (take 7 (drop (- 7 target-day-of-week)
                                              (cycle (range 7 0 -1)))))
                     now-day-of-week)
        new-date (doto (js/Date. (.valueOf now))
                   (.setDate (+ delta-days (.getDate now))))]
    new-date))

(defn add-days [day delta-days]
  (doto (js/Date. (.valueOf day))
    (.setDate (+ delta-days (.getDate day)))))

(defn format-date [date]
  (.format (js/Intl.DateTimeFormat. "en-US" #js {:weekday "short"
                                                 :month "short"
                                                 :day "numeric"})
           date))

(defn hour-rows-view
  [availability hours {:keys [show-events? show-global-availability?]}]
  [:tbody
   (doall
    (for [hour hours]
      ^{:key hour}
      [:tr
       [:td.hour hour ":00"]
       (doall
        (for [day util/days]
          ^{:key day}
          [:td
           (let [value (availability [day hour])
                 global-availability-percent (when show-global-availability?
                                               @(mod/subscribe [:global-availability day hour]))]
             [:button
              {:class (case value
                        :preferred "preferred"
                        :available "available"
                        nil "empty")
               #_#_:style {:background-color (str "hsl(0 0 " (* 100 (- 1 (/ global-availability-percent 4))) "%)") }
               :style {:position "relative"}
               :on-click (fn [_]
                           (mod/dispatch [:set-availability!
                                          [day hour]
                                          (case value
                                            :preferred nil
                                            :available :preferred
                                            nil :available)]))}
              (when (and show-events?
                         (contains? @(mod/subscribe [:next-meetups]) [day hour]))
                [:div {:style {:position "absolute"
                               :top 0
                               :left 5
                               :right 5
                               :color "white"
                               :font-size "0.75em"
                               :text-transform "uppercase"
                               :background "#318a3a"
                               :z-index 100}}
                 "Event"])
              ;; ordered pips
              #_[:div
                 {:style {:position "absolute"
                        :top 0
                        :left 0
                        :right 0
                        :bottom 0
                        :text-wrap "balance"}}
               (repeat global-availability-count
                       [:span.pip
                        {:style {:display "inline-block"
                                 :background "#ccc"
                                 :width "0.5em"
                                 :height "0.5em"
                                 :margin "0.1em"
                                 :border-radius "50%"}}])]
              ;; big oval
              (when global-availability-percent
                (let [size 100]
                  [:svg {:style {:position "absolute"
                                 :z-index 0
                                 :width "100%"
                                 :height "100%"}
                         :preserve-aspect-ratio "none"
                         :view-box (str "0 0 " size " " size)}
                   [:circle {:cx (/ size 2)
                             :cy (/ size 2)
                             :r (* (/ size 2)
                                   ;; sqrt, so that area is linear with value
                                   (Math/sqrt global-availability-percent))
                             :fill "#f3f3f3"}]]))
              ;; random pips
              #_(let [w 43
                    h 16
                    r 1.5]
                [:svg {:style {:position "absolute"
                               :width "100%"
                               :height "100%"
                               :z-index 0
                               :preserve-aspect-ratio "xMidYMid meet"
                               :pointer-events "none"}
                       :view-box (str "0 0 " w " " h)}
                 (repeatedly global-availability-count
                             (fn []
                               [:circle {:cx (+ r (rand-int (- w r r)))
                                         :cy (+ r (rand-int (- h r r)))
                                         :r r
                                         :fill "#eee"}]))])
              [:div.wrapper
               {:style {:z-index 1}}
               #_global-availability-count
               (case value
                 :preferred "P"
                 :available "A"
                 nil "")]])]))]))])

(defn availability-view
  [opts]
  (r/with-let [force-show-early? (r/atom false)
               force-show-late? (r/atom false)]
    [ui/row
     {:title "Availability"
      :info [:<>
             [:div "Click in the calendar grid below to indicate your time availability."]
             [:div "A = available, P = preferred"]]}
     [:<>
      [:div {:tw "absolute right-4 top-4 flex gap-4 items-center"}
       [ui/secondary-button {:on-click (fn [_]
                                         (mod/dispatch [:clear-availability!]))} "Clear all"]
       #_[:div {:tw "flex gap-1 items-center"}
        [fa/fa-globe-solid {:tw "w-4 h-4"}]
        @(mod/subscribe [:user-profile-value :user/time-zone])]]
      (when-let [availability @(mod/subscribe [:user-profile-value :user/availability])]
        [:div {:tw "max-h-100vh overflow-x-auto mt-4"}
         [:table.availability
          [:thead
           [:tr
            [:th {:tw "sticky top-0 bg-white z-100"}]
            (let [next-monday (next-day-of-week (js/Date.) :monday)]
              (for [[i day] (map-indexed (fn [i d] [i d]) util/days)]
                (let [[day-of-week date] (string/split (format-date (add-days next-monday i)) #",")]
                  ^{:key day}
                  [:th.day {:tw "sticky top-0 bg-white"}
                   [:div.day-of-week day-of-week]
                   [:div.date date]])))]]
          (if (or @force-show-early?
                  (some availability (for [hour util/early-hours
                                           day util/days]
                                       [day hour])))
            [:<>
             [:tbody
              [:tr ;; filler row, so that header doesn't cover the offset hour label
               [:td {:col-span 8 :tw "h-1em"} ""]]]
             [hour-rows-view availability util/early-hours opts]]
            [:tbody
             [:tr
              [:td]
              [:td {:col-span 7}
               [:div {:tw "flex justify-center"}
                [ui/secondary-button {:on-click (fn []
                                                  (reset! force-show-early? true))}
                 "Show earlier hours"]]]]])
          [hour-rows-view availability util/hours opts]
          (if (or @force-show-late?
                  (some availability (for [hour util/late-hours
                                           day util/days]
                                       [day hour])))
            [hour-rows-view availability util/late-hours opts]
            [:tbody
             [:tr
              [:td]
              [:td {:col-span 7}
               [:div {:tw "flex justify-center"}
                [ui/secondary-button {:on-click (fn []
                                                  (reset! force-show-late? true))}
                 "Show later hours"]]]]])]])]]))

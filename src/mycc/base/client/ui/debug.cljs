(ns mycc.base.client.ui.debug
  (:require
    [clojure.pprint :as pprint]
    [reagent.core :as r]
    [mycc.base.client.state :as state]))

(def debug?
  ^boolean js/goog.DEBUG)

(defonce open? (r/atom false))

(defn db-view []
  (if @open?
    [:<>
     [:pre {:style {:background "black"
                    :color "white"
                    :position "fixed"
                    :overflow-y "auto"
                    :margin 0
                    :top 0
                    :left 0
                    :bottom 0
                    :z-index 100
                    :padding "1em"}}
      (with-out-str
        (pprint/pprint @state/user))]
    [:button {:style {:position "fixed"
                      :z-index 101
                      :color "white"
                      :bottom 0
                      :left 0}
              :on-click (fn [] (reset! open? false))}
     "CLOSE"]]
    [:button {:style {:position "fixed"
                      :bottom 0
                      :left 0}
              :on-click (fn [] (reset! open? true))}
     "DEBUG"]))

(ns mycc.base.client.ui.login
  (:require
    [clojure.string :as string]
    [reagent.core :as r]
    [re-frame.core :refer [dispatch]]))

(defn rand-in-range [low high]
  (+ low (rand (- high low))))

(defn star-field-view []
  (let [w js/window.innerWidth
        h js/window.innerHeight
        star-count 300
        base-star-size 12]
    [:svg.star-field
     {:xmlns "http://www.w3.org/2000/svg"
      :width "100%"
      :height "100%"
      :viewBox (string/join " " [0 0 w h])}
     (into [:g]
           (repeatedly
             star-count
             (fn []
               [:circle {:cx (rand-in-range 0 w)
                         :cy (rand-in-range 0 h)
                         :r (* base-star-size
                               (rand-nth (concat
                                           (repeat 5 0.05)
                                           (repeat 5 0.075)
                                           (repeat 4 0.01)
                                           (repeat 3 0.15)
                                           (repeat 2 0.02)
                                           (repeat 1 0.025))))
                         :fill "#fff"}])))]))

(defn login-view []
  (let [sent-email (r/atom nil)]
    (fn []
      [:div.login
       [star-field-view]
       [:img.logomark
        {:src "/logomark.svg"
         :alt "Logo of Clojure Camp. A star constellation in the shape of alambda."}]
       [:h1
        [:img.logotype
         {:src "/logotype.svg"
          :alt "Clojure Camp"}]]
       [:form
        {:on-submit (fn [e]
                      (let [email (.. e -target -elements -email -value)]
                        (.preventDefault e)
                        (dispatch [:log-in! email])
                        (reset! sent-email email)))}
        [:label
         "Enter your email:"
         [:input {:name "email"
                  :type "email"}]]
        [:button "Login"]
        (when @sent-email
          [:div "An email with a login-link was sent to " @sent-email])]])))

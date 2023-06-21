(ns dojo.client.ui.login
  (:require
    [reagent.core :as r]
    [re-frame.core :refer [dispatch]]))

(defn login-view []
  (let [sent-email (r/atom nil)]
    (fn []
      [:div.login
       [:img {:src "/logo.svg"
              :alt "A circle with green-blue gradient and two white parentheses in the middle"}]
       [:h1
        [:span {:style {:color "#5FAD31"}} "Clojo"]
        [:span {:style {:color "#567ED2"}} "Dojo"]]
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

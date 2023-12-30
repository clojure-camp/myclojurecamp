(ns mycc.profile.ui
  (:require
    [re-frame.core :refer [dispatch subscribe]]
    [mycc.api :as api]))

(defn name-view []
  [:section.field.name
   [:label.name
    [:h1 "Name"]
    [:input {:type "text"
             :value @(subscribe [:user-profile-value :user/name])
             :on-change (fn [e]
                          (dispatch
                            [:set-user-value! :user/name (.. e -target -value)]))}]]])

(defn profile-page-view []
  [:div.page.profile
   [name-view]])

(api/register-page!
  {:page/id :page.id/profile
   :page/path "/"
   :page/view #'profile-page-view
   :page/nav-label "Profile"
   :page/on-enter! (fn []
                     )})

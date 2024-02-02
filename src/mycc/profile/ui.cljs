(ns mycc.profile.ui
  (:require
    [re-frame.core :refer [dispatch subscribe]]
    [mycc.common.ui :as ui]
    [modulo.api :as mod]))

(defn name-view []
  [ui/row {:title "Name"}
   [ui/input
    {:type "text"
     :value @(subscribe [:user-profile-value :user/name])
     :on-change (fn [e]
                  (dispatch
                    [:set-user-value! :user/name (.. e -target -value)]))}]])

(defn role-view []
  [ui/row {:title "Role"}
   [ui/radio-list
    {:value @(mod/subscribe [:user-profile-value :user/role])
     :choices [[:role/student "Student"]
               [:role/mentor "Mentor"]]
     :on-change (fn [value]
                  (mod/dispatch [:set-user-value! :user/role value]))}]])

(defn profile-page-view []
  [:div.page.profile
   [name-view]
   [role-view]])

(mod/register-page!
  {:page/id :page.id/profile
   :page/path "/"
   :page/view #'profile-page-view
   :page/nav-label "Profile"
   :page/styles [:.page.profile]
   :page/on-enter! (fn [])})

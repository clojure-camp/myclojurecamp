(ns mycc.studygroup.ui
  (:require
    [mycc.common.ui :as ui]
    [modulo.api :as mod]))

(defn interested-studygroup-view []
  [ui/row {:title "Join a Study Group?"}
   [ui/radio-list
    {:value @(mod/subscribe [:user-profile-value :user/interested-studygroup])
     :choices [[:interested-studygroup/yes "Yes"]
               [:interested-studygroup/no "No"]]
     :on-change (fn [value]
                  (mod/dispatch [:set-user-value! :user/interested-studygroup value]))}]])

(defn proficiency-view []
  [ui/row {:title "Clojure Proficiency Level"}
   [ui/radio-list
    {:value @(mod/subscribe [:user-profile-value :user/proficiency])
     :choices [[:proficiency/beginner "Beginner"]
               [:proficiency/intermediate "Intermediate"]]
     :on-change (fn [value]
                  (mod/dispatch [:set-user-value! :user/proficiency value]))}]])

(defn region-view []
  [ui/row {:title "Region"}
   [ui/radio-list
    {:value @(mod/subscribe [:user-profile-value :user/region])
     :choices [[:region/na-east "US/Canada East"]
               [:region/na-west "US/Canada West"]
               [:region/brazil-sa "Brazil/South America"]
               [:region/europe-east "Europe East"]
               [:region/europe-west "Europe West"]
               [:region/other "Africa/Australia/Other"]]
     :on-change (fn [value]
                  (mod/dispatch [:set-user-value! :user/region value]))}]])

(defn goal-view []
  [ui/row {:title "Goal"}
   [ui/radio-list
    {:value @(mod/subscribe [:user-profile-value :user/goal])
     :choices [[:goal/start "Get started with Clojure"]
               [:goal/hobby "Continue learning as a hobby"]
               [:goal/professional "Continue learning for use professionally"]]
     :on-change (fn [value]
                  (mod/dispatch [:set-user-value! :user/goal value]))}]])


(defn studygroup-page-view []
  [:div.page.studygroup
   [ui/row {}
    [:div {:tw "italic leading-relaxed"}
     [:div "If you would like to sign up to participate in a study group, please make selections below. 
            IMPORTANT: Study groups require a 2 hour time commitment every week in order for the group to thrive. 
            If you cannot commit to consistent attendence, please do not sign up at this point."]]]
   [interested-studygroup-view]
   [proficiency-view]
   [region-view]
   [goal-view]])

(mod/register-page!
  {:page/id :page.id/studygroup
   :page/path "/studygroup"
   :page/view #'studygroup-page-view
   :page/nav-label "Study Group"
   :page/styles [:.page.p2p]
   :page/on-enter! (fn [])})

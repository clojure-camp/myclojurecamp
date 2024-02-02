(ns mycc.profile.ui
  (:require
    [mycc.common.ui :as ui]
    [modulo.api :as mod]))

(defn name-view []
  [ui/row {:title "Name"}
   [ui/input
    {:type "text"
     :default-value @(mod/subscribe [:user-profile-value :user/name])
     :on-change (fn [e]
                  (mod/dispatch
                    [:debounced-set-user-value! :user/name (.. e -target -value)]))}]])

(defn role-view []
  [ui/row {:title "Role"}
   [ui/radio-list
    {:value @(mod/subscribe [:user-profile-value :user/role])
     :choices [[:role/student "Student"]
               [:role/mentor "Mentor"]]
     :on-change (fn [value]
                  (mod/dispatch [:set-user-value! :user/role value]))}]])

(defn github-username-view []
  [ui/row {:title "Github Username"}
   [ui/input
    {:type "text"
     :default-value @(mod/subscribe [:user-profile-value :user/github-user])
     :on-change (fn [e]
                  (mod/dispatch
                    [:debounced-set-user-value! :user/github-user (.. e -target -value)]))}]])

(defn discord-username-view []
  [ui/row {:title "Discord Username"}
   [ui/input
    {:type "text"
     :default-value @(mod/subscribe [:user-profile-value :user/discord-user])
     :on-change (fn [e]
                  (mod/dispatch
                    [:debounced-set-user-value! :user/discord-user (.. e -target -value)]))}]])

(defn learner-questions-view []
  (when (= :role/student @(mod/subscribe [:user-profile-value :user/role]))
    [:<>
     (for [[title subtitle k]
           [["Why are you learning Clojure?"
             "What is your primary motivation for learning Clojure? Ex. to get a job doing X, to learn to program, to build a website for yourself, etc."
             :user/profile-why-clojure]
            ["What is your prior experience with programming and programming in Clojure?"
             nil
             :user/profile-programming-experience]
            ["What is your next learning goal?"
             "What are you working on right now? What are you trying to learn? Ex. how to use reduce"
             :user/profile-short-term-milestone]
            ["What is your next major learning goal?"
             "What are you working towards? Ex. creating a website"
             nil
             :user/profile-long-term-milestone]]]
       [ui/row {:title title
                :subtitle subtitle}
        [ui/textarea
         {:default-value @(mod/subscribe [:user-profile-value k])
          :on-change (fn [e]
                       (mod/dispatch
                         [:debounced-set-user-value! k (.. e -target -value)]))}]])]))

(defn profile-page-view []
  [:div.page.profile
   [ui/row {}
    [:div {:tw "italic leading-relaxed"}
     [:div "Welcome to Clojure Camp! Please tell us a little about yourself."]
     [:div "FYI, your profile info will be shared with the Clojure Camp community."]]]
   [name-view]
   [role-view]
   [ui/row {}
    [:p {:tw "italic"} "The rest of these are optional:"]]
   [github-username-view]
   [discord-username-view]
   [learner-questions-view]])

(mod/register-page!
  {:page/id :page.id/profile
   :page/path "/"
   :page/view #'profile-page-view
   :page/nav-label "Profile"
   :page/styles [:.page.profile]
   :page/on-enter! (fn [])})

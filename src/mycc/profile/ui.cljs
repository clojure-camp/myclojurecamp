(ns mycc.profile.ui
  (:require
    [clojure.string :as string]
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

(defn time-zone-view []
  [ui/row
   {:title "Time Zone"}
   [:label {:tw "flex gap-2"}
    [:select {:tw "p-1 bg-white border border-gray-300 font-light"
              :value @(mod/subscribe [:user-profile-value :user/time-zone])
              :on-change (fn [e]
                           (mod/dispatch [:set-user-value! :user/time-zone (.. e -target -value)]))}
     (for [timezone (.supportedValuesOf js/Intl "timeZone")]
       ^{:key timezone}
       [:option {:value timezone} timezone])]
    [ui/button
     {:on-click (fn []
                  (mod/dispatch
                    [:set-user-value! :user/time-zone (.. js/Intl DateTimeFormat resolvedOptions -timeZone)]))}
     "Auto-Detect"]]])

(defn language-views []
  [:<>
   (doall
     (for [[k title info]
           [[:user/primary-languages
             "Primary Languages"
             "Languages you can speak, listen and write fluently."]
            [:user/secondary-languages
             "Secondary Languages"
             "Languages you can get by with, but prefer your primary languages."]]]
       (let [value (or @(mod/subscribe [:user-profile-value k]) #{})]
         ^{:key k}
         [ui/row {:title title
                  :info info}
          [ui/checkbox-list
           {:value value
            :choices (->> (into #{:language/mandarin :language/spanish :language/english
                                  :language/hindi :language/portuguese :language/russian
                                  :language/japanese :language/french :language/polish
                                  :language/bengali :language/arabic}
                                ;; user might have added custom languages
                                value)
                          sort
                          (map (fn [id]
                                 [id (string/capitalize (name id))])))
            :on-change (fn [value]
                         (mod/dispatch [:set-user-value! k value]))}]
          [ui/secondary-button
           {:on-click (fn []
                        (let [in (js/prompt "Language name:")
                              language (keyword "language" (string/lower-case (string/replace in #"\W" "")))]
                          (mod/dispatch [:set-user-value! k (conj value language)])))}
           "+ Language"]])))])

(defn topics-view []
  [ui/row
   {:title "Learning Topics"
    :info [:div
           [:div "Learners - Topics you're interested in learning. Feel free to add your own."]
           [:div "Mentors - Topics you have experience with."]]}
   (doall
     (for [[category topics] (->> @(mod/subscribe [:topics])
                                  (group-by :topic/category)
                                  sort
                                  reverse)]
       ^{:key (or category "other")}
       [:section {:tw "space-y-3"}
        [:h1 {:tw "italic"} (or category "other")]
        [ui/checkbox-list
         {:value @(mod/subscribe [:user-profile-value :user/topic-ids])
          :choices (->> topics
                        (sort-by :topic/name)
                        (map (fn [{:topic/keys [id name]}]
                               [id name])))
          :on-change (fn [_value action changed-value]
                       (case action
                         :add (mod/dispatch [:add-user-topic! changed-value])
                         :remove (mod/dispatch [:remove-user-topic! changed-value])))}]]))
   [ui/secondary-button
    {:on-click (fn [_]
                 (let [value (js/prompt "Enter a new topic:")]
                   (when (not (string/blank? value))
                     (mod/dispatch [:new-topic! (string/trim value)]))))}
    "+ Add Topic"]])

(defn learner-questions-view []
  (when (= :role/student @(mod/subscribe [:user-profile-value :user/role]))
    [:<>
     [ui/row {:title "Motivation"
              :subtitle "Are you learning Clojure for career/job reasons (ex. to get a software development job, get better at your job...), or more for hobby/personal-interest reasons (ex. to build personal projects, contribute to open-source...)"}
      [ui/radio-list
       {:value @(mod/subscribe [:user-profile-value :user/profile-motivation])
        :choices [[:motivation/job "Just Job"]
                  [:motivation/job-leaning "Mostly Job"]
                  [:motivation/balanced "50/50"]
                  [:motivation/hobby-leaning "Mostly hobby"]
                  [:motivation/hobby "Just Hobby"]]
        :on-change (fn [value]
                     (mod/dispatch [:set-user-value! :user/profile-motivation value]))}]]

     [ui/row {:title "General Programming Experience"
              :subtitle "Which of these statements best describes your experience with programming in general?"}
      [ui/radio-list
       {:value @(mod/subscribe [:user-profile-value :user/profile-experience-programming])
        :choices [[0 "I have none"]
                  [1 "I have written a few lines now and again"]
                  [2 "I have written programs for my own use that are a couple of pages long"]
                  [3 "I have written and maintained larger pieces of software"]]
        :direction :vertical
        :on-change (fn [value]
                     (mod/dispatch [:set-user-value! :user/profile-experience-programming value]))}]]

     (when (< 1 @(mod/subscribe [:user-profile-value :user/profile-experience-programming]))
       [ui/row {:title "General Programming Experience - Follow Up"
                :subtitle [:<>
                           "Which of these statements best describes how easily you could write a program to do the following (in any language):"
                           [:div {:tw "mt-1 italic"} "A command-line program to identify all airports more than 4 hops away from a given airport. You are provided an API that returns airports and flights between them."]]}
        [ui/radio-list
         {:value @(mod/subscribe [:user-profile-value :user/profile-experience-programming-example])
          :choices [[0 "I wouldn’t know where to start."]
                    [1 "I could struggle through by trial and error, with a lot of web searches or ChatGPT."]
                    [2 "I could do it quickly with little or no use of external help."]]
          :direction :vertical
          :on-change (fn [value]
                       (mod/dispatch [:set-user-value! :user/profile-experience-programming-example value]))}]])

     [ui/row {:title "Clojure Programming Experience"
              :subtitle [:span "Which of these statements best describes your experience with programming " [:em "in Clojure or Clojurescript?"]]}
      [ui/radio-list
       {:value @(mod/subscribe [:user-profile-value :user/profile-experience-clojure])
        :choices [[0 "I have none"]
                  [1 "I have written a few lines now and again"]
                  [2 "I have written programs for my own use that are a couple of pages long"]
                  [3 "I have written and maintained larger pieces of software"]]
        :direction :vertical
        :on-change (fn [value]
                     (mod/dispatch [:set-user-value! :user/profile-experience-clojure value]))}]]

     (when (< 1 @(mod/subscribe [:user-profile-value :user/profile-experience-clojure]))
       [ui/row {:title "Clojure Programming Experience - Follow Up"
                :subtitle [:<>
                           "Which of these statements best describes how easily you could write a program to do the following (in Clojure):"
                           [:div {:tw "mt-1 italic"} "A command-line program to identify all airports more than 4 hops away from a given airport. You are provided an API that returns airports and flights between them."]]}
        [ui/radio-list
         {:value @(mod/subscribe [:user-profile-value :user/profile-experience-clojure-example])
          :choices [[0 "I wouldn’t know where to start."]
                    [1 "I could struggle through by trial and error, with a lot of web searches or ChatGPT."]
                    [2 "I could do it quickly with little or no use of external help."]]
          :direction :vertical
          :on-change (fn [value]
                       (mod/dispatch [:set-user-value! :user/profile-experience-clojure-example value]))}]])

     (doall
       (for [[title subtitle k]
             [["What is your next learning goal?"
               "What are you working on right now? What are you trying to learn? Ex. how to use reduce, how to allow users to log in. (You can meet with a mentor to clarify this.)"
               :user/profile-short-term-milestone]
              ["What is your next major learning goal?"
               "What are you working towards? Ex. finish an Advent-of-Code Day 1 on my own, create an app for playing a simple game. (You can meet with a mentor to clarify this.)"
               :user/profile-long-term-milestone]]]
         ^{:key k}
         [ui/row {:title title
                  :subtitle subtitle}
          [ui/textarea
           {:default-value @(mod/subscribe [:user-profile-value k])
            :on-change (fn [e]
                         (mod/dispatch
                           [:debounced-set-user-value! k (.. e -target -value)]))}]]))]))

(defn profile-page-view []
  [:div.page.profile
   [ui/row {}
    [:div {:tw "italic leading-relaxed"}
     [:div "Welcome to Clojure Camp! Please tell us a little about yourself."]
     [:div "FYI, your profile info will be shared with the Clojure Camp community."]]]
   [name-view]
   [role-view]
   [discord-username-view]
   #_[github-username-view]
   [ui/row {}
    [:p {:tw "italic"} "The rest of these are optional, but help us plan content and events:"]]
   [time-zone-view]
   [language-views]
   [topics-view]
   [learner-questions-view]])

(mod/register-page!
  {:page/id :page.id/profile
   :page/path "/"
   :page/view #'profile-page-view
   :page/nav-label "Profile"
   :page/styles [:.page.profile]
   :page/on-enter! (fn []
                     (mod/dispatch [:p2p/fetch-topics!]))})

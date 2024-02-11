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

;; programming level
;;   beginner
;;   intermediate

;; 0  I'm just starting out
;; 1  I can write a simple program - ex. counting characters, sum a list
;; 2  I can write a multi-step program - ex. ~Advent of Code 1-5
;; 3  I have worked on or written a 1000+ line non-trivial program
;; 4  i have built clojure applications, a non-trivial library, worked a FT clojure job for multiple years

;; clojure programming level
;;   beginner - just starting
;;   intermediate
;;   intermediate 2
;;   expert - i have built clojure applications, a non-trivial library, worked a FT clojure job for multiple years;  I could write a clojure application that:

;; why learning clojure?
;;   career/job
;;   .
;;   50/50
;;   .
;;   personal/hobby

(defn topics-view []
  [ui/row
   {:title "Learning Topics"
    :info [:div
           [:div "Learners - Topics you're interested in learning. Feel free to add your own."]
           [:div "Mentors - Topics you have experience with."]]}
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
                       :remove (mod/dispatch [:remove-user-topic! changed-value])))}]])
   [ui/secondary-button
    {:on-click (fn [_]
                 (let [value (js/prompt "Enter a new topic:")]
                   (when (not (string/blank? value))
                     (mod/dispatch [:new-topic! (string/trim value)]))))}
    "+ Add Topic"]])

(defn learner-motivation-view []
  (when (= :role/student @(mod/subscribe [:user-profile-value :user/role]))
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
                    (mod/dispatch [:set-user-value! :user/profile-motivation value]))}]]))

(defn learner-questions-view []
  (when (= :role/student @(mod/subscribe [:user-profile-value :user/role]))
    [:<>
     (doall
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
   [ui/row {}
    [:p {:tw "italic"} "The rest of these are optional:"]]
   [language-views]
   [topics-view]
   [github-username-view]
   [learner-motivation-view]
   [learner-questions-view]])

(mod/register-page!
  {:page/id :page.id/profile
   :page/path "/"
   :page/view #'profile-page-view
   :page/nav-label "Profile"
   :page/styles [:.page.profile]
   :page/on-enter! (fn []
                     (mod/dispatch [:p2p/fetch-topics!]))})

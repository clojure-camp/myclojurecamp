(ns mycc.common.profile
  (:require
   [clojure.string :as string]
   [mycc.common.ui :as ui]
   [modulo.api :as mod]))

(defn role-view []
  [ui/row {:title "Role"}
   [ui/radio-list
    {:value @(mod/subscribe [:user-profile-value :user/role])
     :choices [[:role/student "Student"]
               [:role/mentor "Mentor"]]
     :on-change (fn [value]
                  (mod/dispatch [:set-user-value! :user/role value]))}]])

(def languages
  #{:language/mandarin :language/spanish :language/english
    :language/hindi :language/portuguese :language/russian
    :language/japanese :language/french :language/polish
    :language/bengali :language/arabic})

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
            :choices (->> (into languages
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
                              language (string/lower-case (string/replace in #"\W" ""))]
                          (when (not (string/blank? language))
                            (let [language (keyword "language" language)]
                              (mod/dispatch [:set-user-value! k (conj value language)])))))}
           "+ Language"]])))])

(defn multi-state-checkbox [{:keys [value states label on-change]}]
  [:div {:tw "inline-block cursor-pointer"
         :on-click on-change}
   (if value
     [:div.box {:tw "inline-block mr-1 border border-blue-500 rounded text-white bg-blue-500 px-0.5"}
      (let [value-index (.indexOf states value)]
        (into [:<>]
              (for [[index v] (map-indexed vector states)]
                (if (<= index value-index)
                  "●"
                  "○"))))]
     [:div.box {:tw "inline-block mr-1 border border-gray-500 rounded text-white px-0.5"}
      "○○○"])
   label])

(defn multi-state-checkbox-list
  [{:keys [choices on-change direction]}]
  [:div.multi-state-checkbox-list {:tw ["flex gap-y-3 flex-wrap gap-x-4"
                                        (when (= direction :vertical)
                                          "flex-col")]}
   (let [states [:level/beginner :level/intermediate :level/expert]]
     (for [[topic choice-value] choices]
       ^{:key (:topic/id topic)}
       [multi-state-checkbox {:value choice-value
                              :states states
                              :label (:topic/name topic)
                              :on-change (fn [] (on-change (:topic/id topic)
                                                           (->> (cycle (conj states nil))
                                                                (drop-while (partial not= choice-value))
                                                                second)))}]))])

(defn topics-view []
  [ui/row
   {:title "Learning Topics"
    :subtitle (case @(mod/subscribe [:user-profile-value :user/role])
                :role/student
                "Topics you're interested in learning. Feel free to add your own."
                :role/mentor
                "Topics you have experience with."
                nil)}
   [:div {:tw "space-y-5"}
    (let [category-order ["clojure concepts"
                          "general programming concepts"
                          "clojure libraries and related"
                          "programming practices"
                          "programming domains"
                          "web dev related"
                          nil]
          user-topics @(mod/subscribe [:user-profile-value :user/topics])]
      (doall
       (for [[category topics] (->> @(mod/subscribe [:topics])
                                    (group-by :topic/category)
                                    (sort-by (fn [[k _]]
                                               ;; sort by category-order, then alphabetically
                                               [(let [i (.indexOf category-order k)]
                                                  ;; if not in above list, put at the end
                                                  (if (= -1 i)
                                                    100
                                                    i))
                                                k])))]
         ^{:key (or category "other")}
         [:section {:tw "space-y-3"}
          [:h1 {:tw "italic"} (or category "other")]
          [multi-state-checkbox-list
           {:choices
            (->> topics
                 (sort-by :topic/name)
                 (map (fn [{:topic/keys [id] :as topic}]
                        [topic (user-topics id) ])))
            :on-change (fn [topic-id level]
                         (mod/dispatch [:set-user-topic-level! topic-id level]))}]])))]
   [ui/secondary-button
    {:on-click (fn [_]
                 (let [value (js/prompt "Enter a new topic:")]
                   (when (not (string/blank? value))
                     (mod/dispatch [:new-topic! (string/trim value)]))))}
    "+ Add Topic"]])

(defn time-zone-view []
  [ui/row
   {:title "Time Zone"}
   [:label {:tw "flex gap-2"}
    [:select {:tw "p-1 bg-white border border-gray-300 font-light"
              :value @(mod/subscribe [:user-profile-value :user/time-zone])
              :on-change (fn [e]
                           (mod/dispatch [:set-user-value! :user/time-zone (.. e -target -value)]))}
     (for [timezone (->> (.supportedValuesOf js/Intl "timeZone")
                         ;; Firefox includes these, but Java does not recognize them
                         (remove #{"HST" "MST" "EST" "Factory"}))]
       ^{:key timezone}
       [:option {:value timezone} timezone])]
    [ui/button
     {:on-click (fn []
                  (mod/dispatch
                   [:set-user-value! :user/time-zone (.. js/Intl DateTimeFormat resolvedOptions -timeZone)]))}
     "Auto-Detect"]]])

(ns mycc.community.core
  (:require
    [clojure.string :as string]
    [modulo.api :as mod]
    #?@(:clj
         [[mycc.common.db :as db]
          [lambdaisland.hiccup :as h]]
         :cljs
         [[mycc.common.ui :as ui]])))

(defn experience-view [subtype u]
  (let [q1 {0 "I have none"
            1 "I have written a few lines now and again"
            2 "I have written programs for my own use that are a couple of pages long"
            3 "I have written and maintained larger pieces of software"}
        q2 {0 "I wouldn’t know where to start."
            1 "I could struggle through by trial and error, with a lot of web searches or ChatGPT."
            2 "I could do it quickly with little or no use of external help."}
        [k1 k2] (case subtype
                  :programming
                  [:user/profile-experience-programming :user/profile-experience-programming-example]
                  :clojure
                  [:user/profile-experience-clojure :user/profile-experience-clojure-example])]
    (when (u k1)
      [:div {:style {:display "flex"}}
       [:div {:title (-> u k1 q1)}
        (repeat (+ 1 (u k1)) "●")
        (repeat (- 3 (u k1)) "○")]
       (when (u k2)
         [:<>
          "/"
          [:div {:title (str "How easily solve airport problem?\n" (-> u k2 q2))}
           (repeat (+ 1 (u k2)) "●")
           (repeat (- 2 (u k2)) "○")]])])))

(defn user-tbody-view
  [user id->topic]
  [:tbody
   (for [[label f] [["Name" (fn [u]
                              [:div {:style {:font-weight "bold"}}
                               (:user/name u)])]
                    ["Role" (comp name :user/role)]
                    ["Time Zone" :user/time-zone]
                    ["Languages" (fn [u]
                                   (when (seq (concat (:user/primary-languages u) (:user/secondary-languages u)))
                                     [:div
                                      (interpose ", "
                                                 (concat
                                                   (for [l (:user/primary-languages u)]
                                                     [:span (name l)])
                                                   (for [l (:user/secondary-languages u)]
                                                     [:span {:style {:font-style "italic"}} (name l)])))]))]
                    ["Motivation" (fn [u]
                                    (some-> u :user/profile-motivation name))]
                    ["Programming Experience" (partial experience-view :programming)]
                    ["Clojure Experience" (partial experience-view :clojure)]
                    ["Short Term Learning Milestone" :user/profile-short-term-milestone]
                    ["Long Term Learning Milestone" :user/profile-long-term-milestone]
                    ["Topics" (fn [user]
                                (interpose ", "
                                           (for [t (map id->topic (:user/topic-ids user))]
                                             (:topic/name t))))]
                    ["Github" :user/github-user]
                    ["Discord" :user/discord-user]]]
     (let [out (f user)]
       (when (and out (if (sequential? out) (seq out) true))
         [:tr
          [:td {:style {:padding "0.25em"
                        :vertical-align "top"}} label]
          [:td {:style {:padding "0.25em"
                        :vertical-align "top"
                        :font-weight "lighter"}} (f user)]])))
   [:tr [:td [:div {:style {:height "2em"}}]]]])

#?(:clj
   (do
     (defn community-page-view []
       (let [topics (db/get-entities :topic)
             id->topic (zipmap (map :topic/id topics)
                               topics)
             grouped-users (->> (db/get-users)
                                (group-by (fn [u]
                                            {:role (:user/role u)
                                             :active? (boolean (or (seq (:user/pair-opt-in-history u))
                                                                   (:user/pair-next-week? u)))})))
             groups [{:role :role/student :active? true}
                     {:role :role/mentor :active? true}
                     {:role :role/student :active? false}
                     {:role :role/mentor :active? false}]]
           [:table
            (for [{:keys [role active?] :as group} groups]
              [:<>
               [:tbody
                [:tr
                 [:td {:colspan 2}
                  [:div {:style {:font-size "1.5em"
                                 :padding "0.25rem"
                                 :margin "1rem 0"}}
                   (string/capitalize (name role)) "s" " - " (if active? "Pairing This Week" "Not Pairing")]]]]
               (for [user (->> (grouped-users group)
                               (sort-by :user/name))]
                 [user-tbody-view user id->topic])])]))

     (mod/register-cqrs!
       :community/queries
       [{:id :community/html
         :route [:get "/api/community"]
         :params {:user-id uuid?}
         :conditions
         (fn [{:keys [user-id]}]
           [[#(db/entity-file-exists? :user user-id) :not-allowed "User with this ID does not exist."]])
         :return (fn [_]
                   {:content (h/render
                               [community-page-view]
                               {:doctype? false})})}]))
   :cljs
   (do
     (defn community-page-view
       []
       [ui/server-html-view
        {:route "/api/community"}])

     (mod/register-page!
       {:page/id :page.id/community
        :page/path "/community"
        :page/nav-label "community"
        :page/view #'community-page-view})))

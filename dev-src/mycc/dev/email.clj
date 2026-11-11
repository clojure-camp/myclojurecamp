(ns mycc.dev.email
  (:require
    [bloom.omni.auth.token :as token]
    [modulo.api :as mod]
    [mycc.common.db :as db]
    [mycc.common.email :as email]))

(defn visited-since?
  [user instant]
  (boolean
    (some-> (:user/last-visited-at user)
            .toInstant
            (.isAfter instant))))

(defn pairing-interested-users []
  (let [six-months-ago (-> (java.time.ZonedDateTime/now)
                           (.minusMonths 6)
                           .toInstant)]
    (->> (db/get-users)
         (filter (fn [user]
                   (or
                     (seq (:user/pair-opt-in-history user))
                     (contains? (:user/events-interested-in user) :event/pairing)
                     (:user/subscribed? user)
                     (visited-since? user six-months-ago)))))))

#_(count (pairing-interested-users))

(defn mentor? [user]
  (= :role/mentor (:user/role user)))

(defn split-by-mentor [users]
  {:mentors (filter mentor? users)
   :non-mentors (remove mentor? users)})


#_(count (filter mentor? (pairing-interested-users)))
(defn email-users!
  [{:keys [users subject user->body]}]
  (->> users
       (map (fn [user]
              (email/send! {:to (:user/email user)
                            :unsubscribe-user-id (:user/id user)
                            :subject subject
                            :body (user->body user)})))
       dorun))

(defn login-url
  [user path]
  (str (mod/config :app-domain) path "?"
       (token/login-query-string (:user/id user) (mod/config :auth-token-secret))))

(def pairwithme-update-email
  {:subject "Update to Pairing @ Clojure Camp"
   :user->body (fn [user]
                 [:div
                  [:p "Hey " (:user/name user) ","]
                  [:p "Based on feedback, I've re-worked how pairing works at Clojure Camp."]
                  [:p "Anyone interested in pairing can now add themself to a list on "
                   [:a {:href (login-url user "/pairwithme")} "my.clojure.camp/pairwithme"]
                   " and find others interested in pairing. You can set your availability as before, but instead of auto-matching, you can find others interested in pairing and invite them to pair with you."]
                  [:p "Pairing doesn't have to be just about learning. If you have any projects you're maintaining, this could be a way to find new contributors, or, just as a way to meet and connect with other Clojurians."]
                  [:p "I've added some hours for myself, so you are welcome to pair with me if you'd like."]
                  [:p "Please give it a try and let me know if there are any issues."]
                  [:p "May your parens stay balanced," [:br] "Raf"]])})

#_(email-users! (assoc pairwithme-update-email
                       :users (remove mentor? (pairing-interested-users))))

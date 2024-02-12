(ns mycc.base.schema
  (:require
    [clojure.string :as string]
    [malli.util :as mu]
    [malli.core :as m]))

(def NonBlankString
  [:and
   :string
   [:fn #(not (string/blank? %))]])

(def User
  [:map
   ;; profile
   [:user/name NonBlankString]
   [:user/role
    [:enum
     :role/student
     :role/mentor]]
   [:user/github-user
    [:maybe NonBlankString]]
   [:user/discord-user
    [:maybe NonBlankString]]
   [:user/profile-motivation
    [:maybe [:enum
             :motivation/job
             :motivation/job-leaning
             :motivation/balanced
             :motivation/hobby-leaning
             :motivation/hobby]]]
   [:user/profile-experience-programming
    [:maybe [:and :int [:>= 0] [:<= 3]]]]
   [:user/profile-experience-clojure
    [:maybe [:and :int [:>= 0] [:<= 3]]]]
   [:user/profile-experience-programming-example
    [:maybe [:and :int [:>= 0] [:<= 2]]]]
   [:user/profile-experience-clojure-example
    [:maybe [:and :int [:>= 0] [:<= 2]]]]
   [:user/profile-short-term-milestone
    [:maybe NonBlankString]]
   [:user/profile-long-term-milestone
    [:maybe NonBlankString]]
   ;; pairing
   [:user/pair-with
    [:enum
     :pair-with/only-mentors
     :pair-with/prefer-mentors
     nil
     :pair-with/prefer-students
     :pair-with/only-students]]
   [:user/max-pair-per-day
    [:and :int [:>= 1] [:<= 24]]]
   [:user/max-pair-per-week
    [:and :int [:>= 1] [:<= (* 24 7)]]]
   [:user/max-pair-same-user
    [:and :int [:>= 1] [:<= 50]]]
   [:user/primary-languages [:set :keyword]]
   [:user/secondary-languages [:set :keyword]]
   [:user/time-zone
    [:and
     :string
     [:fn
      (fn [x]
        #?(:cljs true
           :clj
           (try
             (java.time.ZoneId/of x)
             (catch Exception _
               false))))]]]])

(defn allowed-key?
  [schema k]
  (boolean (mu/get schema k)))

(defn valid-key-value?
  [schema k v]
  (m/validate (mu/get schema k) v))

#_(valid-key-value? User :user/name "")
#_(valid-key-value? User :user/max-pair-per-week 0)
#_(valid-key-value? User :user/time-zone "America/Toronto")

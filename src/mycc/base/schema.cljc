(ns mycc.base.schema
  (:require
    [clojure.string :as string]
    [malli.util :as mu]
    [malli.core :as m]
    #?@(:clj
         [[malli.registry :as mr]
          [malli.experimental.time :as mt]])))

#?(:clj
   (mr/set-default-registry!
     (mr/composite-registry
       (m/default-schemas)
       (mt/schemas))))

(def NonBlankString
  [:and
   :string
   [:fn #(not (string/blank? %))]])

(def Language
  [:and
   :keyword
   [:fn (fn [k]
          (= "language" (namespace k)))]])

(def User
  [:map
   [:user/id uuid?]
   ;; profile
   [:user/name NonBlankString]
   [:user/created-at inst?]
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
   [:user/primary-languages [:set Language]]
   [:user/secondary-languages [:set Language]]
   [:user/subscribed? :boolean]
   [:user/pair-next-week? :boolean]
   [:user/pair-opt-in-history [:set
                               #?(:clj :time/local-date
                                  :cljs :any)]]
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
#_(valid-key-value? User :user/created-at (java.util.Date.))
#_(valid-key-value? User :user/pair-opt-in-history #{(java.time.LocalDate/now)})

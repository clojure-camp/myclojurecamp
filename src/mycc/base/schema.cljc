(ns mycc.base.schema
  (:require
    [clojure.string :as string]
    [malli.core :as m]
    [malli.util :as mu]
    [modulo.api :as mod]
    [mycc.p2p.util :as p2p.util]))

(def NonBlankString
  [:and
   :string
   [:fn #(not (string/blank? %))]])

(def SingleLineString
  ;; control chars (ex. newlines) in names could inject email headers or ICS properties
  [:and
   NonBlankString
   [:fn (fn [value]
          (not (re-find #"[\x00-\x1F\x7F]" value)))]])

(def Language
  [:and
   :keyword
   [:fn (fn [k]
          (= "language" (namespace k)))]])

(mod/register-entity!
  :entity/user
  [[:user/id :uuid]
   [:user/email NonBlankString]
   [:user/email-validated? {:optional true} :boolean]
   ;; profile
   [:user/name {:user-editable? true} SingleLineString]
   [:user/created-at inst?]
   [:user/last-visited-at inst?]
   [:user/events-interested-in {:user-editable? true}
    [:maybe
     [:set
      [:enum
       :event/mob-sessions
       :event/pairing]]]]
   [:user/role {:user-editable? true}
    [:enum
     :role/student
     :role/mentor]]
   [:user/github-user {:user-editable? true}
    [:maybe NonBlankString]]
   [:user/discord-user {:user-editable? true}
    [:maybe NonBlankString]]
   [:user/profile-motivation {:user-editable? true}
    [:maybe [:enum
             :motivation/job
             :motivation/job-leaning
             :motivation/balanced
             :motivation/hobby-leaning
             :motivation/hobby]]]
   [:user/profile-experience-programming {:user-editable? true}
    [:maybe [:and :int [:>= 0] [:<= 3]]]]
   [:user/profile-experience-clojure {:user-editable? true}
    [:maybe [:and :int [:>= 0] [:<= 3]]]]
   [:user/profile-experience-programming-example {:user-editable? true}
    [:maybe [:and :int [:>= 0] [:<= 2]]]]
   [:user/profile-experience-clojure-example {:user-editable? true}
    [:maybe [:and :int [:>= 0] [:<= 2]]]]
   [:user/profile-short-term-milestone {:user-editable? true}
    [:maybe NonBlankString]]
   [:user/profile-long-term-milestone {:user-editable? true}
    [:maybe NonBlankString]]
   ;; pairing
   [:user/pair-with {:user-editable? true}
    [:enum
     :pair-with/only-mentors
     :pair-with/prefer-mentors
     nil
     :pair-with/prefer-students
     :pair-with/only-students]]
   [:user/max-pair-per-day {:user-editable? true}
    [:and :int [:>= 1] [:<= 24]]]
   [:user/max-pair-per-week {:user-editable? true}
    [:and :int [:>= 1] [:<= (* 24 7)]]]
   [:user/max-pair-same-user {:user-editable? true}
    [:and :int [:>= 1] [:<= 50]]]
   [:user/primary-languages {:user-editable? true} [:set Language]]
   [:user/secondary-languages {:user-editable? true} [:set Language]]
   [:user/subscribed? {:user-editable? true} :boolean]
   [:user/pair-next-week? :boolean]
   [:user/pairwithme-show-profile? {:user-editable? true :optional true} :boolean]
   ;; "I'd like to pair on..."
   [:user/pairwithme-topics {:user-editable? true :optional true} [:maybe :string]]
   [:user/topics {:optional true}
    [:map-of
     :uuid
     [:maybe
      [:enum
       :level/beginner
       :level/intermediate
       :level/expert]]]]
   [:user/availability {:optional true}
    [:map-of
     [:tuple
      (into [:enum] p2p.util/days)
      (into [:enum] (concat p2p.util/early-hours p2p.util/hours p2p.util/late-hours))]
     [:maybe (into [:enum] (remove nil? p2p.util/availability-values))]]]
   [:user/user-pair-deny-list {:optional true} [:set :user/id]]
   [:user/pair-opt-in-history [:set
                               #?(:clj :time/local-date
                                  :cljs :any)]]
   [:user/time-zone {:user-editable? true}
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

(defn ->map-schema
  ;; entity keywords are registry refs; malli.util needs the :map itself
  [schema]
  (m/deref-all (m/schema schema)))

(defn allowed-key?
  [schema k]
  (-> (mu/find (->map-schema schema) k)
      second
      :user-editable?
      boolean))

(defn valid-key-value?
  [schema k v]
  (m/validate (mu/get (->map-schema schema) k) v))

#_(valid-key-value? :entity/user :user/name "")
#_(valid-key-value? :entity/user :user/max-pair-per-week 0)
#_(valid-key-value? :entity/user :user/time-zone "America/Toronto")
#_(valid-key-value? :entity/user :user/created-at (java.util.Date.))
#_(valid-key-value? :entity/user :user/pair-opt-in-history #{(java.time.LocalDate/now)})

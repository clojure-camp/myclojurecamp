(ns mycc.p2p.util
  (:require
    [clojure.string :as string])
  #?(:clj (:import
            (java.time ZonedDateTime ZoneId)
            (java.time.format DateTimeFormatter))))

(def hours (range 9 21))

(def days [:monday :tuesday :wednesday :thursday :friday :saturday :sunday])

(def availability-values #{:preferred :available nil})

(defn random-availability []
  (into {}
        (for [day days
              hour hours]
          [[day hour] (rand-nth [:preferred :available nil])])))

(defn flag-other-user [add? event user-id]
  (let [f (if add? conj disj)
        other-user-id (first (disj (:event/guest-ids event) user-id))]
    (update event :event/flagged-guest-ids (fnil f #{}) other-user-id)))

(defn ->date-string [at]
  #?(:cljs (-> (.toISOString at) (string/split "T") first)
     :clj (.format (ZonedDateTime/ofInstant (.toInstant at)
                                            (ZoneId/of "UTC"))
                   (DateTimeFormatter/ofPattern "yyyy-MM-dd"))))

(defn ->event-url [event]
  "Discord"
  #_(str "https://meet.jit.si/" "clojure-camp-" (->date-string (:event/at event)) "-" (:event/id event)))

#_(->event-url {:event/guest-ids #{(:user/id (first (db/get-users)))
                                   (:user/id (last (db/get-users)))}
                :event/at #inst "2021-11-08T14:00:00.000-00:00"
                :event/id #uuid "22675d48-b361-4598-b447-4a23b492f4fc"})

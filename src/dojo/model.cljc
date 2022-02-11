(ns dojo.model)

(def hours (range 9 21))

(def days [:monday :tuesday :wednesday :thursday :friday])

(def availability-values #{:preferred :available nil})

(defn random-availability []
  (into {}
        (for [day days
              hour hours]
          [[day hour] (rand-nth [:preferred :available nil])])))

(defn flag-other-user [event user-id]
  (let [other-user-id (first (disj (:event/guest-ids event) user-id))]
    (update event :event/flagged-guest-ids (fnil conj #{}) other-user-id)))

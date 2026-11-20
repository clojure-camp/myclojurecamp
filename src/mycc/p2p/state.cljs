(ns mycc.p2p.state
  (:require
    [reagent.core :as r]
    [modulo.api :as mod]
    [mycc.base.client.state :as state]))

(defonce events (r/atom nil))

(defn fetch-events! []
  (-> (mod/tada! [:events] :quiet? true)
      (.then (fn [data]
               (reset! events data)))))

(defn opt-in-for-pairing! [bool]
  (swap! state/user assoc :user/pair-next-week? bool)
  (mod/tada! [:opt-in-for-pairing! {:value bool}]))

(defn avoid-user! [user-id bool]
  (swap! state/user update :user/user-pair-deny-list (if bool conj disj) user-id)
  (mod/tada! [:avoid-user! {:avoid-user-id user-id
                            :value bool}]))

(ns mycc.pairwithme.state
  (:require
    [reagent.core :as r]
    [modulo.api :as mod]
    [mycc.pairwithme.schema :as schema]))

(defonce sessions (r/atom nil))

;; {:type ... :message ...}
(defonce banner (r/atom nil))

;; {:other-user {...} :session-id (optional, for counter-proposals) :selected-slots #{epoch-ms} :schedule [...]}
;; slots are stored as epoch-ms, because js/Date isn't hashed by value
(defonce modal (r/atom nil))

(defn clear-banner! []
  (reset! banner nil))

;; modal

(defn open-modal! [{:keys [other-user session-id]}]
  (reset! modal {:other-user other-user
                 :session-id session-id
                 :selected-slots #{}
                 :schedule nil})
  (-> (mod/tada! [:pairwithme/schedule {:other-user-id (:user/id other-user)}] :quiet? true)
      (.then (fn [schedule]
               (swap! modal assoc :schedule schedule)))))

(defn close-modal! []
  (reset! modal nil))

(defn toggle-slot! [slot-ms]
  (let [selected (:selected-slots @modal)]
    (cond
      (contains? selected slot-ms)
      (swap! modal update :selected-slots disj slot-ms)

      (< (count selected) schema/max-slots-per-proposal)
      (swap! modal update :selected-slots conj slot-ms))))

;; sessions

(defn maybe-open-counter! [session-id]
  (let [session (->> @sessions
                     (filter (fn [session]
                               (= session-id (:pairwithme-session/id session))))
                     first)]
    (if (= :respond (:status session))
      (open-modal! {:other-user (:other-user session)
                    :session-id session-id})
      (reset! banner {:type :success
                      :message "This proposal is no longer open. See your sessions below."}))))

(defn fetch-sessions! [{:keys [open-counter-for]}]
  (-> (mod/tada! [:pairwithme/sessions] :quiet? true)
      (.then (fn [data]
               (reset! sessions data)
               (when open-counter-for
                 (maybe-open-counter! open-counter-for))))))

(defn propose! []
  (let [{:keys [other-user session-id selected-slots]} @modal
        slots (->> selected-slots
                   sort
                   (mapv (fn [slot-ms] (js/Date. slot-ms))))]
    (swap! modal assoc :sending? true)
    (-> (mod/tada! (if session-id
                     [:pairwithme/counter-propose! {:session-id session-id
                                                    :slots slots}]
                     [:pairwithme/propose! {:other-user-id (:user/id other-user)
                                            :slots slots}]))
        (.then (fn [_]
                 (reset! modal nil)
                 (reset! banner {:type :success
                                 :message (str "Proposal sent to " (:user/name other-user) " by email. You will get an email when they accept or reply.")})
                 (fetch-sessions! {})))
        (.catch (fn [_]
                  (swap! modal assoc
                         :sending? false
                         :error "Could not send. The times may no longer be valid. Close and try again."))))))

;; accept

(defn accept! [{:keys [session-id proposal-id slot-index]}]
  (reset! banner {:type :pending
                  :message "Accepting..."})
  (-> (mod/tada! [:pairwithme/accept! {:session-id session-id
                                       :proposal-id proposal-id
                                       :slot-index slot-index}])
      (.catch (fn [_]
                {:error :not-found}))
      (.then (fn [result]
               (reset! banner (assoc result :type :accept))
               (fetch-sessions! {})))))

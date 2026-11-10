(ns modulo.client.tada
  (:require
   [bloom.commons.ajax :as ajax]
   [reagent.core :as r]
   [reagent.ratom :as ratom]))

(defonce pending-requests (r/atom #{}))

(defn saved? []
  (empty? @pending-requests))

(defn ajax-promise! [opts]
  (js/Promise.
   (fn [resolve reject]
     (ajax/request (assoc opts
                          :on-success resolve
                          :on-error reject)))))

(defn send! [[event-id params]]
  ;; NOTE: Sending all params even though that may sometimes unnecessary,
  ;; but backend just ignores extra params
  (ajax-promise! {:uri (str "/api/tada/" (namespace event-id) "." (name event-id))
                  :method :post
                  :params {:tada.event/id event-id
                           :tada.event/params (or params {})}}))

(defn tada!
  "Sends event, showing pending state and alerting on failure.
   Use {:quiet? true} for neither (ex. for queries)."
  [event & {:keys [quiet?]}]
  (if quiet?
    (send! event)
    (let [request-id (gensym "request")]
      (swap! pending-requests conj request-id)
      (-> (send! event)
          (.then (fn [data]
                   (swap! pending-requests disj request-id)
                   data))
          (.catch (fn [error]
                    (swap! pending-requests disj request-id)
                    (js/alert "Error sending data to server. Your latest changed may not have been saved.")
                    (throw error)))))))

;; not using metadata on the value, because nil (initial value) can't hold metadata
(defonce refresh-fns (js/WeakMap.))

(defn tada-atom
  [[event-id params] & {:keys [refresh-rate]}]
  (let [ratom (ratom/atom nil)
        fetch! (fn []
                 (-> (send! [event-id params])
                     (.then (fn [data]
                              (reset! ratom data)))))
        interval (when refresh-rate (js/setInterval fetch! refresh-rate))
        reaction (ratom/make-reaction (fn []
                                        @ratom)
                                      :on-dispose
                                      (fn []
                                        (js/clearInterval interval)))]
    (fetch!)
    (.set refresh-fns reaction fetch!)
    reaction))

(defn refresh! [reaction]
  ((.get refresh-fns reaction)))

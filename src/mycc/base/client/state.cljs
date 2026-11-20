(ns mycc.base.client.state
  (:require
    [clojure.string :as string]
    [bloom.commons.ajax :as ajax]
    [reagent.core :as r]
    [modulo.api :as mod]))

(defonce user (r/atom nil))

(defonce checked-auth? (r/atom false))

(defn user-value [k]
  (get @user k))

(defn set-user-value! [k v]
  (swap! user assoc k v)
  (mod/tada! [:set-profile-value! {:k k
                                   :v (if (and (string? v) (string/blank? v))
                                        nil
                                        v)}]))

(defonce debounce-timeouts (atom {}))

(defn debounced-set-user-value! [k v]
  (js/clearTimeout (@debounce-timeouts k))
  (swap! debounce-timeouts assoc k (js/setTimeout
                                     (fn []
                                       (set-user-value! k v))
                                     500)))

(defn maybe-set-time-zone! []
  (when (nil? (user-value :user/time-zone))
    (set-user-value! :user/time-zone (.. js/Intl DateTimeFormat resolvedOptions -timeZone))))

(defn fetch-user! []
  (-> (mod/tada! [:user] :quiet? true)
      (.then (fn [data]
               (reset! user data)
               (mod/tada! [:mark-visited!])
               (maybe-set-time-zone!)))
      (.catch (fn [_]))
      (.finally (fn []
                  (reset! checked-auth? true)))))

(defn log-in! [email]
  (mod/tada! [:request-login-link-email! {:email email
                                          :return-to js/window.location.pathname}]))

(defn log-out! []
  (ajax/request {:method :delete
                 :uri "/api/session"
                 :on-success (fn []
                               (reset! user nil))}))

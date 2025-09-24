(ns mycc.common.gcal
  (:require
   [mycc.common.date :as date]
   [modulo.api :as mod]
   [cheshire.core :as json]
   [org.httpkit.client :as http]))

(def base-url "https://www.googleapis.com/calendar/v3/")

(defn calendar-events [{:keys [calendar-id]}]
  (let [now (java.util.Date.)
        ;; we don't need to be exact here,
        ;; so this date manipulation heresy is fine
        next-month (java.util.Date.
                    (+ (.getTime now)
                       (* 31 24 60 60 1000)))
        req {:method  :get
             :url (str base-url "calendars/" calendar-id "/events")
             :query-params {:key (mod/config [:google-api-key])
                            :eventTypes "default"
                            :timeMax (date/format next-month
                                                  "yyyy-MM-dd'T'HH:mm:ssZZZZZ")
                            :timeMin (date/format now
                                                  "yyyy-MM-dd'T'HH:mm:ssZZZZZ")}
             :headers {"Content-Type" "application/json"}}
        res @(http/request req)]
    (if (<= 200 (:status res) 299)
      (-> res
          :body
          (json/parse-string true)
          :items)
      (println res))))

(comment
  (calendar-events {:calendar-id "admin@clojure.camp"}))

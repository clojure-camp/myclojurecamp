(ns dojo.config
  (:require
    [bloom.commons.config :as config]))

(def schema
  [:map
   [:http-port integer?]
   [:app-domain [:and
                 string?
                 #_[:re #"^https?://.*$"]]]
   [:environment [:enum :dev :prod]]
   [:auth-cookie-secret string?]
   [:auth-token-secret string?]
   [:emails [:vector string?]]
   [:data-path string?]
   [:smtp-credentials
    [:map
     [:port integer?]
     [:host string?]
     [:ssl boolean?]
     [:from string?]
     [:user string?]
     [:pass string?]]]])

(def config
  (delay (config/read "config.edn" schema)))

(defn generate []
  (spit "config.edn"
     {:app-domain "http://localhost:8025"
      :auth-cookie-secret "1234567890123456"
      :auth-token-secret "1234567890123456"
      :data-path "external"
      :environment :dev
      :http-port 8025
      :smtp-credentials {:from ""
                         :host ""
                         :pass ""
                         :port 0
                         :ssl false
                         :user ""}}))

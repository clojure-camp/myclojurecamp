(ns mycc.base.config
  (:require
    [clojure.java.io :as io]
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
   [:data-path string?]
   [:smtp-credentials
    [:map
     [:port integer?]
     [:host string?]
     [:tls boolean?]
     [:from string?]
     [:user string?]
     [:pass string?]]]])

(defn generate! []
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
                            :tls false
                            :user ""}}))

(def config
  (delay
    (when (not (.exists (io/file "config.edn")))
      (println "No config.edn detected, creating a default file.")
      (generate!))
    (config/read "config.edn" schema)))

#_(deref config)

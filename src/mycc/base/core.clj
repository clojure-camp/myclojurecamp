(ns mycc.base.core
  (:require
    [mycc.base.cqrs] ;; for side-effects
    [modulo.api :as mod]))

(def config-schema
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

(def config-default
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
                      :user ""}})

(defn start! []
  (mod/start!
    {:config-schema config-schema
     :config-default config-default
     :page-title "Clojure Camp"
     :cookie-name "clojurecamp"}))

(defn stop! []
  (mod/stop!))

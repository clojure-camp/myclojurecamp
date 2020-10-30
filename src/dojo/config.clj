(ns dojo.config
  (:require
    [bloom.commons.config :as config]))

(def schema
  [:map
   [:http-port integer?]
   [:app-domain [:and
                 string?
                 [:re #"^https?://.*$"]]]
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
  (config/read "config.edn" schema))

#_(config/generate "config.edn" schema)



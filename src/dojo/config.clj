(ns dojo.config
  (:require
    [bloom.commons.config :as config]))

(def config
  (config/read
   "config.edn"
   [:map
    [:http-port integer?]
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
      [:pass string?]]]]))

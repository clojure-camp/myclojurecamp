(ns dojo.config
  (:require
    [bloom.commons.config :as config]))

(def config
  (config/read
   "config.edn"
   [:map
    [:emails [:vector string?]]
    [:smtp-credentials
     [:map
      [:port integer?]
      [:host string?]
      [:ssl boolean?]
      [:from string?]
      [:user string?]
      [:pass string?]]]]))

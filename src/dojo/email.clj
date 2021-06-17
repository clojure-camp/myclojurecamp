(ns dojo.email
  (:require
    [postal.core :as postal]
    [hiccup.core :as hiccup]
    [dojo.config :refer [config]]))

(defn send! [{:keys [to subject body]}]
  (println body)
  (postal/send-message
    (:smtp-credentials @config)
    {:from (:from (:smtp-credentials @config))
     :to to
     :subject subject
     :body [{:type "text/html; charset=utf-8"
             :content (hiccup/html body)}]}))

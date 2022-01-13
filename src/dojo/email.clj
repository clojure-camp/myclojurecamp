(ns dojo.email
  (:require
    [postal.core :as postal]
    [hiccup.core :as hiccup]
    [dojo.config :refer [config]]))

(defn send!
  [{:keys [to subject body attachments]}]
  (println body)
  (try
    (postal/send-message
      (:smtp-credentials @config)
      {:from (:from (:smtp-credentials @config))
       :to to
       :subject subject
       :body (concat [{:type "text/html; charset=utf-8"
                       :content (hiccup/html body)}]
                     attachments)})
    (catch com.sun.mail.util.MailConnectException _
      (println "Couldn't send email."))))

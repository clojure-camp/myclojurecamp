(ns dojo.emails.login-link
  (:require
    [bloom.omni.auth.token :as token]
    [dojo.config :refer [config]]))

(defn login-email-template [user]
  {:to (:user/email user)
   :subject "[ClojoDojo] Your Login Link"
   :body
   [:div
    [:p "Hi " (:user/name user) ","]
    [:p "To log in to ClojoDojo, "
     [:a {:href
          (str (@config :app-domain)
               "/?" (token/login-query-string (:user/id user) (@config :auth-token-secret)))} "click here"] "."]]})

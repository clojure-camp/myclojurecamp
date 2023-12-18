(ns mycc.emails.login-link
  (:require
    [bloom.omni.auth.token :as token]
    [mycc.config :refer [config]]))

(defn login-email-template [user]
  {:to (:user/email user)
   :subject "Clojure Camp - Your Login Link"
   :body
   [:div
    [:p "Hi " (:user/name user) ","]
    [:p "To log in to Clojure Camp, "
     [:a {:href
          (str (@config :app-domain)
               (if (:user/email-validated? user)
                "/"
                "/validate")
               "?" (token/login-query-string (:user/id user) (@config :auth-token-secret)))} "click here"] "."]]})

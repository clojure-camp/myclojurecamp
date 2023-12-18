(ns mycc.base.login-link-email
  (:require
    [bloom.omni.auth.token :as token]
    [mycc.api :as api]))

(defn login-email-template [user]
  {:to (:user/email user)
   :subject "Clojure Camp - Your Login Link"
   :body
   [:div
    [:p "Hi " (:user/name user) ","]
    [:p "To log in to Clojure Camp, "
     [:a {:href
          (str (api/config :app-domain)
               (if (:user/email-validated? user)
                "/"
                "/validate")
               "?" (token/login-query-string (:user/id user) (api/config :auth-token-secret)))} "click here"] "."]]})

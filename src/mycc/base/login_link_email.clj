(ns mycc.base.login-link-email
  (:require
    [bloom.omni.auth.token :as token]
    [modulo.api :as mod]))

(defn login-email-template [user]
  {:to (:user/email user)
   :subject "Clojure Camp - Your Login Link"
   :body
   [:div
    [:p "Hi " (:user/name user) ","]
    [:p "To log in to Clojure Camp, "
     [:a {:href
          (str (mod/config :app-domain)
               (if (:user/email-validated? user)
                "/"
                "/validate")
               "?" (token/login-query-string (:user/id user) (mod/config :auth-token-secret)))} "click here"] "."]]})

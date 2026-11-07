(ns mycc.base.login-link-email
  (:require
    [bloom.omni.auth.token :as token]
    [modulo.api :as mod]
    [mycc.common.return-to :as return-to])
  (:import
    (java.net URLEncoder)))

(defn login-email-template
  ([user]
   (login-email-template user nil))
  ([user return-path]
   (let [return-path (if (return-to/safe? return-path)
                       return-path
                       "/")
         token-query (token/login-query-string (:user/id user) (mod/config :auth-token-secret))]
     {:to (:user/email user)
      :subject "Clojure Camp - Your Login Link"
      :body
      [:div
       [:p "Hi " (:user/name user) ","]
       [:p "To log in to Clojure Camp, "
        [:a {:href
             (str (mod/config :app-domain)
                  (if (:user/email-validated? user)
                    (str return-path "?" token-query)
                    (str "/validate?return-to=" (URLEncoder/encode return-path "UTF-8") "&" token-query)))}
         "click here"] "."]]})))

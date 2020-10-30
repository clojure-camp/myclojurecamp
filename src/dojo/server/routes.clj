(ns dojo.server.routes
  (:require
    [dojo.db :as db]
    [dojo.email :as email]
    [dojo.emails.login-link :as emails.login-link]))

(def routes
  [
   [[:put "/api/request-login-link-email"]
    (fn [request]
      (let [email (get-in request [:body-params :email])
            user (or (db/get-user-by-email email)
                     (db/create-user! email))]
        (email/send!
          (emails.login-link/login-email-template user))))]

   [[:get "/api/user"]
    (fn [request]
      {:body (db/get-user
               (get-in request [:session :user-id]))})]

   [[:put "/api/user/update-availability"]
    (fn [request]
      (let [{:keys [hour day value]} (request :body-params)]
        (-> (db/get-user (get-in request [:session :user-id]))
            (assoc-in [:user/availability [day hour]] value)
            db/save-user!))
      {:status 200})]

   [[:delete "/api/session"]
    (fn [request]
      {:status 200
       :session nil})]])

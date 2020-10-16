(ns dojo.server.routes
  (:require
    [dojo.db :as db]
    [bloom.omni.auth.token :as token]
    [dojo.config :refer [config]]))

(defn login-email-template [user-id]
  (println
    (str "/?" (token/login-query-string user-id (config :auth-token-secret)))))

(def routes
  [
   [[:get "/api/auth"]
    (fn [_]
      {:body {:ok 1}
       :session {:user-id user-id}})]

   [[:put "/api/request-login-link-email"]
    (fn [request]
      (let [email (get-in request [:body-params :email])
            user (or (db/get-user-by-email email)
                     (db/create-user! email))]
        (login-email-template (:user/id user))))]

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
      {:status 200})]])

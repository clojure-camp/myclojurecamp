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

   [[:get "/api/topics"]
    (fn [_]
      {:body (db/get-topics)})]

   [[:put "/api/topics"]
    (fn [request]
      {:body (db/create-topic! (get-in request [:body-params :name]))})]

   [[:put "/api/user/add-topic"]
    (fn [request]
      (some-> (db/get-user (get-in request [:session :user-id]))
              (update :user/topic-ids conj (get-in request [:body-params :topic-id]))
              db/save-user!)
      {:status 200})]

   [[:put "/api/user/remove-topic"]
    (fn [request]
      (some-> (db/get-user (get-in request [:session :user-id]))
              (update :user/topic-ids disj (get-in request [:body-params :topic-id]))
              db/save-user!)
      {:status 200})]

   [[:put "/api/user/update-availability"]
    (fn [request]
      (let [{:keys [hour day value]} (request :body-params)]
        (some-> (db/get-user (get-in request [:session :user-id]))
                (assoc-in [:user/availability [day hour]] value)
                db/save-user!))
      {:status 200})]

   [[:put "/api/user/opt-in-for-pairing"]
    (fn [request]
      (let [{:keys [value]} (request :body-params)]
        (some-> (db/get-user (get-in request [:session :user-id]))
                (assoc :user/pair-next-week? value)
                db/save-user!))
      {:status 200})]

   [[:delete "/api/session"]
    (fn [request]
      {:status 200
       :session nil})]])

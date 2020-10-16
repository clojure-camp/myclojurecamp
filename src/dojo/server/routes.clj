(ns dojo.server.routes
  (:require
    [dojo.db :as db]))

(def routes
  [
   [[:get "/api/auth"]
    (fn [_]
      {:body {:ok 1}
       :session {:user-id #uuid "4c884a18-95a8-4e9e-b69b-754f7773e93a"}})]

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

(ns modulo.routes
  (:require
    [bloom.omni.impl.ring :as omni.ring]
    [tada.events.ring]
    [tada.events.core :as tada]
    [modulo.cqrs-registry :as cqrs]
    ;; TODO db should be under modulo, or passed in via opts to modulo.api/start!
    [mycc.common.db :as db]))

(defonce dynamic-cqrs-handler
  (atom (fn [_request])))

(defn params-middleware [handler]
  (fn [request]
    ;; TADA wants a :params key on requests
    (handler (assoc request :params
              (assoc (:body-params request)
                     :user-id (get-in request [:session :user-id]))))))

(defonce watcher
  (cqrs/watch!
    :mycc.base.routes
    (fn [commands-and-queries]
      (tada/register! commands-and-queries)
      (reset! dynamic-cqrs-handler
              (omni.ring/->handler
                (->> commands-and-queries
                     (map (fn [{:keys [id route]}]
                            [route
                             (tada.events.ring/route id)
                             [params-middleware]]))))))))

#_(tada/do :request-login-link-email! {:email "foo@example.com"})

(def routes
  [[[:get "/validate"]
    (fn [request]
      ;; NOTE: a GET request with a side effect
      ;; first time after registering, users get sent here
      (some-> (get-in request [:session :user-id])
              (db/get-user)
              (assoc :user/email-validated? true)
              db/save-user!)
      {:status 302
       :headers {"Location" "/"}})]

   [[:delete "/api/session"]
    (fn [_]
      {:status 200
       :session nil})]

   [[:any "/*"]
    (fn [request]
      (@dynamic-cqrs-handler request))]])

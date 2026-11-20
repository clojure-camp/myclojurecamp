(ns modulo.routes
  (:require
    [bloom.omni.impl.ring :as omni.ring]
    [tada.events.ring]
    [tada.events.core :as tada]
    [modulo.cqrs-registry :as cqrs]
    ;; TODO db should be under modulo, or passed in via opts to modulo.api/start!
    [mycc.common.db :as db]
    [mycc.common.return-to :as return-to]))

(defonce dynamic-cqrs-handler
  (atom (fn [_request])))

(defn params-middleware [handler]
  (fn [request]
    ;; TADA wants a :params key on requests
    (handler (assoc request :params
              (merge (update-keys (:query-params request) keyword)
                     (:body-params request)
                     {:user-id (get-in request [:session :user-id])})))))

(defonce watcher
  (cqrs/watch!
    :mycc.base.routes
    (fn [commands-and-queries]
      (tada/register! cqrs/t commands-and-queries)
      (reset! dynamic-cqrs-handler
              (omni.ring/->handler
                (->> commands-and-queries
                     (filter :route)
                     (map (fn [{:keys [id route]}]
                            [route
                             (tada.events.ring/route cqrs/t id)
                             [params-middleware]]))))))))

#_(tada/do! cqrs/t :request-login-link-email! {:email "foo@example.com"})

(defn tada-handler
  [request]
  (let [{:tada.event/keys [id params]} (request :body-params)]
    (if (and (keyword? id)
             (map? params))
      (-> (tada.events.ring/ring-dispatch-event!
            cqrs/t
            id
            (assoc params :user-id (get-in request [:session :user-id])))
          ((fn [response]
             (if (string? (:body response))
               (assoc-in response [:headers "Content-Type"] "text/plain")
               response))))
      {:status 400
       :headers {"Content-Type" "text/plain"}
       :body "Incorrect TADA event"})))

(def routes
  [[[:get "/validate"]
    (fn [request]
      ;; NOTE: a GET request with a side effect
      ;; first time after registering, users get sent here
      (some-> (get-in request [:session :user-id])
              (db/get-user)
              (assoc :user/email-validated? true)
              db/save-user!)
      (let [return-path (get-in request [:query-params "return-to"])]
        {:status 302
         :headers {"Location" (if (return-to/safe? return-path)
                                return-path
                                "/")}}))]

   ;; suffix is only for readability in logs and devtools
   [[:post "/api/tada/*"]
    tada-handler]

   [[:delete "/api/session"]
    (fn [_]
      {:status 200
       :session nil})]

   [[:any "/*"]
    (fn [request]
      (@dynamic-cqrs-handler request))]])

(ns mycc.omni-config
  (:require
    [mycc.config :refer [config]]
    [mycc.server.routes :as routes]))

(defn omni-config []
  {:omni/http-port (@config :http-port)
   :omni/title "Clojure Camp"
   :omni/environment (@config :environment)
   :omni/cljs {:main "mycc.client.core"}
   :omni/auth (-> {:cookie {:name "clojurecamp"
                            :secret (@config :auth-cookie-secret)}
                   :token {:secret (@config :auth-token-secret)}})
   :omni/api-routes #'routes/routes})

(def omni-config-prod (omni-config))

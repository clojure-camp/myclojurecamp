(ns mycc.base.omni-config
  (:require
    [mycc.base.config :refer [config]]
    [mycc.base.routes :as routes]))

(defn omni-config []
  {:omni/http-port (@config :http-port)
   :omni/title "Clojure Camp"
   :omni/environment (@config :environment)
   :omni/cljs {:main "mycc.base.client.core"}
   :omni/auth (-> {:cookie {:name "clojurecamp"
                            :secret (@config :auth-cookie-secret)}
                   :token {:secret (@config :auth-token-secret)}})
   :omni/api-routes #'routes/routes})

(def omni-config-prod (omni-config))

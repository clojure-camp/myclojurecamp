(ns dojo.omni-config
  (:require
    [dojo.config :refer [config]]
    [dojo.server.routes :as routes]))

(def omni-config
  {:omni/http-port (config :http-port)
   :omni/title "ClojoDojo"
   :omni/environment (config :environment)
   :omni/cljs {:main "dojo.client.core"}
   :omni/auth (-> {:cookie {:name "clojodojo"
                            :secret (config :auth-cookie-secret)}
                   :token {:secret (config :auth-token-secret)}})
   :omni/api-routes #'routes/routes})


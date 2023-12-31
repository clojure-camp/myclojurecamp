(ns modulo.system
  (:require
    [bloom.omni.core :as omni]
    [modulo.config :as config]
    [modulo.routes :as routes]
    [modulo.jobs :as jobs]))

(defn set-default-exception-handler
  []
  (Thread/setDefaultUncaughtExceptionHandler
   (reify Thread$UncaughtExceptionHandler
     (uncaughtException [_ thread ex]
       (println ex "Uncaught exception " (.getName thread))))))

(defn omni-config [opts]
  {:omni/http-port (config/config :http-port)
   :omni/title (opts :page-title)
   :omni/environment (config/config :environment)
   :omni/cljs {:main "mycc.base.client.core"}
   :omni/auth (-> {:cookie {:name (opts :cookie-name)
                            :secret (config/config :auth-cookie-secret)}
                   :token {:secret (config/config :auth-token-secret)}})
   :omni/api-routes #'routes/routes})

(def prod-omni-config
  ;; due to present limitations in omni, this has to be statically defined
  ;; but, we don't need most values
  {:omni/environment :prod
   :omni/cljs {:main "mycc.base.client.core"}})

(defn start! [opts]
  (set-default-exception-handler)
  (config/initialize! (opts :config-schema)
                      (opts :config-default))
  (omni/start! omni/system (omni-config opts))
  (jobs/initialize!))

(defn stop! []
  (omni/stop!))

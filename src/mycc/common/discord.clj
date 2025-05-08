(ns mycc.common.discord
  (:require
    [modulo.api :as mod]
    [cheshire.core :as json]
    [org.httpkit.client :as http]))

(def base-url "https://discord.com/api/v10")

(defn discord-request [{:keys [url]}]
  (let [req {:method  :get
             :url (str base-url url)
             :headers {"Authorization" (str "Bot " (mod/config [:discord :token]))
                       "Content-Type" "application/json"
                       "User-Agent" "Discord Bot (http://localhost, 0.1)"}}
        res @(http/request req)]
   (if (<= 200 (:status res) 299)
     (-> res
         :body
         (json/parse-string true)
         ))))

(defn list-guilds []
  (discord-request {:url "/users/@me/guilds"}))

(defn list-guild-events [guild-id]
  (discord-request {:url (str "/guilds/" guild-id "/scheduled-events")}))

(comment
  (map #(select-keys % [:name :id :scheduled_start_time :recurrence_rule])
    (list-guild-events (-> (list-guilds) first :id))))


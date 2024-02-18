(ns mycc.stats.cqrs
  (:require
    [modulo.api :as mod]
    [mycc.common.db :as db]
    [mycc.stats.ui :as ui]
    [lambdaisland.hiccup :as h]))

(def commands
  [])

(def queries
  [{:id :stats/all
    :route [:get "/api/stats/all"]
    :params {:user-id uuid?}
    :conditions
    (fn [{:keys [user-id]}]
      [[#(db/entity-file-exists? :user user-id) :not-allowed "User with this ID does not exist."]])
    :return (fn [_]
              {:content (h/render
                          [ui/reports-view
                            {:users (db/get-users)
                             :topics (db/get-entities :topic)
                             :events (db/get-entities :event)}]
                          {:doctype? false})})}])

(mod/register-cqrs! :stats/commands commands)
(mod/register-cqrs! :stats/queries queries)


(ns mycc.admin.cqrs
  (:require
    [modulo.api :as mod]
    [mycc.common.db :as db]
    [mycc.admin.ui :as ui]
    [lambdaisland.hiccup :as h]))

(def commands
  [])

(def queries
  [{:id :admin/all
    :route [:get "/api/admin/all"]
    :params {:user-id uuid?}
    :conditions
    (fn [{:keys [user-id]}]
      [[#(db/entity-file-exists? :user user-id) :not-allowed "User with this ID does not exist."]
       ;; TODO is admin
       ])
    :return (fn [_]
              {:content (h/render
                          [ui/reports-view
                            {:users (db/get-users)
                             :topics (db/get-entities :topic)
                             :events (db/get-entities :event)}]
                          {:doctype? false})})}])

(mod/register-cqrs! :admin/commands commands)
(mod/register-cqrs! :admin/queries queries)


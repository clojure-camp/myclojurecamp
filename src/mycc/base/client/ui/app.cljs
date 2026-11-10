(ns mycc.base.client.ui.app
  (:require
    [garden.core :as garden]
    [modulo.api :as mod]
    [mycc.common.colors :as colors]
    [mycc.base.client.state :as state]
    [mycc.base.client.styles :refer [styles]]
    [mycc.base.client.ui.main :refer [main-view]]
    [mycc.base.client.ui.login :refer [login-view]]))

(defonce favicon
  (let [element (.createElement js/document "link")]
    (.setAttribute element "rel" "icon")
    (.setAttribute element "href" "/logomark.svg")
    (.appendChild (.querySelector js/document "head") element)
    nil))

(defn app-view []
  [:<>
   [:style
    (garden/css (concat styles
                        (->> @(mod/pages)
                             vals
                             (keep :page/styles))))
    (colors/tailwind-color-styles)]

   (cond
     @state/user
     [main-view]

     @state/checked-auth?
     [login-view])])


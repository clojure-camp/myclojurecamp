(ns mycc.base.client.ui.app
  (:require
    [garden.core :as garden]
    [re-frame.core :refer [subscribe]]
    [modulo.api :as mod]
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
                             (keep :page/styles))))]

   (cond
     @(subscribe [:user])
     [main-view]

     @(subscribe [:checked-auth?])
     [login-view])])


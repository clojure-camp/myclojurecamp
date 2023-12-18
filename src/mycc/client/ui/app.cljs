(ns mycc.client.ui.app
  (:require
    [garden.core :as garden]
    [re-frame.core :refer [subscribe]]
    [mycc.client.styles :refer [styles]]
    [mycc.client.ui.main :refer [main-view]]
    [mycc.client.ui.login :refer [login-view]]))

(defonce favicon
 (let [element (.createElement js/document "link")]
   (.setAttribute element "rel" "icon")
   (.setAttribute element "href" "/logomark.svg")
   (.appendChild (.querySelector js/document "head") element)
   nil))

(defn app-view []
  [:<>
   [:style
    (garden/css styles)]

   (cond
     @(subscribe [:user])
     [main-view]

     @(subscribe [:checked-auth?])
     [login-view])])


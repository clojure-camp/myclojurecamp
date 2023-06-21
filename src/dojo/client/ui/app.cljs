(ns dojo.client.ui.app
  (:require
    [garden.core :as garden]
    [re-frame.core :refer [subscribe]]
    [dojo.client.styles :refer [styles]]
    [dojo.client.ui.main :refer [main-view]]
    [dojo.client.ui.login :refer [login-view]]))

(defonce favicon
 (let [element (.createElement js/document "link")]
   (.setAttribute element "rel" "icon")
   (.setAttribute element "href" "/logo.svg")
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


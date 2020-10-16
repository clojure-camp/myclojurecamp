(ns dojo.seed
  (:require
    [bloom.commons.uuid :as uuid]
    [dojo.model :as model]
    [dojo.db :as db]))

(defn seed! []
  (db/save-user!
    {:user/id (uuid/random)
     :user/availability (model/random-availability)}))

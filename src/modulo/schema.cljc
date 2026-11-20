(ns modulo.schema
  (:require
    [malli.core :as m]
    [malli.registry :as mr]
    #?@(:clj
         [[malli.experimental.time :as mt]])))

(defonce registry
  (atom {}))

#_(keys @registry)

(mr/set-default-registry!
  (mr/composite-registry
    (m/default-schemas)
    #?(:clj (mt/schemas))
    (mr/mutable-registry registry)))

(defn ->attribute-entry
  ;; [:user/x {:optional true} :boolean] -> [:user/x {:optional true}]
  [[k & more]]
  (if (= 2 (count more))
    [k (first more)]
    k))

(defn register-entity!
  "attributes: [[:user/id :uuid] [:user/x {:optional true} :boolean] ...]"
  [entity-key attributes]
  (swap! registry
         (fn [registry]
           (-> registry
               (merge (->> attributes
                           (map (fn [[k & more]]
                                  [k (last more)]))
                           (into {})))
               (assoc entity-key
                 (into [:map] (map ->attribute-entry attributes)))))))

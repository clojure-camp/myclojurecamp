(ns modulo.cqrs-registry)

(defonce cqrs-registry
  (atom {}))

#_(deref cqrs-registry)

(defn all [a]
  (->> a
       vals
       (apply concat)))

#_(map :id (all @cqrs-registry))

(defn register-cqrs!
  [id-key value]
  (swap! cqrs-registry assoc id-key value))

(defn watch!
  [id-key f]
  (f (all @cqrs-registry))
  (add-watch
    cqrs-registry
    id-key
    (fn [_ _ _ new-state]
      (f (all new-state)))))

(ns mycc.base.jobs)

(defonce jobs
  (atom {}))

(defn register-job! [id f]
  (swap! jobs assoc id f))

(defn initialize! []
  (doseq [[k f] @jobs]
    (println "Initializing job: " k)
    (f))
  ;; return nil, b/c output for jobs stalls some REPLs
  nil)

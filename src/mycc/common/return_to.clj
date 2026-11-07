(ns mycc.common.return-to)

(defn safe?
  ;; only local paths, so the link can't redirect to another site
  [path]
  (boolean
    (and (string? path)
         (re-matches #"/[A-Za-z0-9/_-]*" path)
         (not (.startsWith path "//")))))

#_(safe? "/pairwithme/accept/1/2/0")
#_(safe? "//evil.com")

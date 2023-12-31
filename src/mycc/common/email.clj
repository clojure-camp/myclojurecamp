(ns mycc.common.email
  (:require
    [postal.core :as postal]
    [hiccup.core :as hiccup]
    [modulo.api :as mod]))

(defn strip-classes-and-ids [k]
  (keyword (first (clojure.string/split (name k) #"[.#]"))))

#_(strip-classes-and-ids :p.event)

(defn textify [content]
  ;; TODO should normalize all vector nodes to include {}
  (clojure.walk/postwalk
    (fn [node]
      #_(println node)
      (cond
        (and (vector? node) (not (map-entry? node)))
        (case (strip-classes-and-ids (first node))
          :br
          "\n"
          :a
          (str (clojure.string/join "" (drop 2 node)) " (" (:href (second node)) ")")
          :p
          (str "\n" (clojure.string/join "" (rest node)) "\n")
          :div
          (str (clojure.string/join "" (rest node)) "\n")
          ; else
          (clojure.string/join "" (rest node)))
        (seq? node)
        (clojure.string/join "" node)
        :else
        node))
    content))

(defn send!
  [{:keys [to subject body attachments]}]
  (println (textify body))
  (try
    (postal/send-message
      (mod/config :smtp-credentials)
      {:from (:from (mod/config :smtp-credentials))
       :to to
       :subject subject
       :List-Unsubscribe "<mailto: unsubscribe@clojure.camp?subject=unsubscribe>"
       :body (concat [[:alternative
                       {:type "text/plain; charset=utf-8"
                        :content (textify body)}
                       {:type "text/html; charset=utf-8"
                        :content (hiccup/html [:html [:body body]])}]]
                     attachments)})
    (catch com.sun.mail.util.MailConnectException _
      (println "Couldn't send email."))))

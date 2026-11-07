(ns mycc.common.email
  (:require
    [lambdaisland.hiccup :as hiccup]
    [modulo.api :as mod]
    [postal.core :as postal]))

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
  [{:keys [to reply-to message-id headers unsubscribe-user-id subject body attachments]}]
  (println (textify body))
  (try
    (postal/send-message
      (mod/config :smtp-credentials)
      (merge headers
             (when reply-to
               {:reply-to reply-to})
             (when message-id
               ;; postal expects a fn
               {:message-id (constantly message-id)})
             {:from (:from (mod/config :smtp-credentials))
              :to to
              :subject subject
              :body (concat [[:alternative
                              {:type "text/plain; charset=utf-8"
                               :content (textify body)}
                              {:type "text/html; charset=utf-8"
                               :content (hiccup/render [:html [:body body]] {:doctype? false})}]]
                            attachments)}
             (if unsubscribe-user-id
               {:List-Unsubscribe
                (str "<https://my.clojure.camp/api/p2p/unsubscribe/?uid=" unsubscribe-user-id ">")
                :List-Unsubscribe-Post "List-Unsubscribe=One-Click"}
               {:List-Unsubscribe "<mailto: unsubscribe@clojure.camp?subject=unsubscribe>"})))
    (catch com.sun.mail.util.MailConnectException _
      (println "Couldn't send email."))))

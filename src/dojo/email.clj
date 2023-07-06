(ns dojo.email
  (:require
    [postal.core :as postal]
    [hiccup.core :as hiccup]
    [dojo.config :refer [config]]))

(defn strip-classes-and-ids [k]
  (keyword (first (clojure.string/split (name k) #"[.#]"))))

#_(strip-classes-and-ids :p.event)


(defn textify
  "Given hiccup content, strips out html tags and returns remaining text as string"
  [content]
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
      (:smtp-credentials @config)
      {:from (:from (:smtp-credentials @config))
       :to to
       :subject subject
       :List-Unsubscribe "<mailto: unsubscribe@clojodojo.com?subject=unsubscribe>"
       :body (concat [[:alternative
                       {:type "text/plain; charset=utf-8"
                        :content (textify body)}
                       {:type "text/html; charset=utf-8"
                        :content (hiccup/html [:html [:body body]])}]]
                     attachments)})
    (catch com.sun.mail.util.MailConnectException _
      (println "Couldn't send email."))))




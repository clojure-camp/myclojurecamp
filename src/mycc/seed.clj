(ns mycc.seed
  (:require
   [bloom.commons.uuid :as uuid]
   [mycc.p2p.util :as util]
   [mycc.p2p.db :as p2p.db]
   [mycc.common.db :as db]))

(def topics
  {"programming domains"
   ["artificial-intelligence-machine-learning"
    "data-science"
    "deploying-web-app-to-production"
    "game-development"
    "generative-art"
    "mobile-development"
    "monte-carlo-simulation"
    "music"
    "web-development"]

   "programming concepts"
   ["debugging"
    "git"
    "sql"]

   "clojure concepts"
   ["macros"]

   "clojure libraries and related"
   ["babashka"
    "biff"
    "clojure-dart"
    "clojure.spec"
    "clojurescript"
    "core.async"
    "datomic"
    "hiccup"
    "malli"
    "re-frame"
    "reagent"
    "reitit"
    "shadow-cljs"
    "specter"
    "tick"]})

(defn seed! []
  (let [topics (flatten
                (for [[category topics] topics]
                  (for [topic topics]
                    {:topic/id (uuid/random)
                     :topic/name topic
                     :topic/category category})))
        users [{:user/id (uuid/random)
                :user/name "Alice"
                :user/email "alice@example.com"
                :user/primary-languages #{:language/english}
                :user/secondary-languages #{:language/german}
                :user/time-zone "America/Toronto"
                :user/topics (zipmap (take 10 (shuffle (map :topic/id topics)))
                                     (repeat :level/beginner))
                :user/role :role/student
                :user/availability (util/random-availability)}
               {:user/id (uuid/random)
                :user/name "Bob"
                :user/email "bob@example.com"
                :user/primary-languages #{:language/english}
                :user/secondary-languages #{:language/french}
                :user/time-zone "Europe/Dublin"
                :user/topics (zipmap (take 10 (shuffle (map :topic/id topics)))
                                     (repeat :level/expert))
                :user/role :role/mentor
                :user/availability (util/random-availability)}]]
    (doseq [topic topics]
      (p2p.db/save-topic! topic))
    (doseq [user users]
      (db/save-user! user))))

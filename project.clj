(defproject dojo "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [jarohen/chime "0.3.2"]
                 [com.draines/postal "2.0.3"]
                 [io.bloomventures/omni "0.26.2"]
                 [io.bloomventures/commons "0.10.7"]

                 [re-frame "0.10.5"]
                 [reagent "0.10.0"]
                 [org.clojure/clojurescript "1.10.764"]
                 [hiccup "1.0.5"]]

  :plugins [[io.bloomventures/omni "0.25.4"]]

  :omni-config dojo.omni-config/omni-config

  :main dojo.core
  :repl-options {:init-ns dojo.core}

  :profiles {:uberjar
             {:aot :all
              :prep-tasks [["omni" "compile"]
                           "compile"]}})

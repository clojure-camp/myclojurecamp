(defproject mycc "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [jarohen/chime "0.3.2"]
                 [com.draines/postal "2.0.5"]
                 [io.bloomventures/omni "0.34.0"]
                 [tada/tada "0.2.2"]
                 [re-frame/re-frame "0.10.5"]
                 [com.lambdaisland/hiccup "0.0.33"]
                 ;; pairing algorithm
                 [org.clojure/math.combinatorics "0.1.6"]
                 [org.clojure/data.csv "1.0.0"]
                 ;; these we get from omni, override some:
                 [io.bloomventures/commons "0.14.1"]
                 ;; [garden/garden "1.3.10"]
                 ;; [reagent/reagent "0.10.0"]
                 ;; [org.clojure/clojurescript "1.10.764"]
                 ;; [hiccup/hiccup "1.0.5"]
                 ]

  :plugins [[io.bloomventures/omni "0.34.0"]]

  :omni-config modulo.system/prod-omni-config

  :main mycc.core
  :repl-options {:init-ns mycc.dev
                 :timeout 200000}

  :jvm-opts ["-Dclojure.tools.logging.factory=clojure.tools.logging.impl/jul-factory"]

  :profiles {:dev
             {:source-paths ["src" "dev-src"]}
             :uberjar
             {:aot :all
              :prep-tasks [["omni" "compile"]
                           "compile"]}})

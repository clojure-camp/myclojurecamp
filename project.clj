(defproject dojo "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [jarohen/chime "0.3.2"]
                 [com.draines/postal "2.0.3"]
                 [io.bloomventures/commons "0.10.5"]
                 [hiccup "1.0.5"]]
  :main dojo.core
  :repl-options {:init-ns dojo.core})

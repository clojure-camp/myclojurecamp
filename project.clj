(defproject dojo "0.1.0-SNAPSHOT"

  :plugins [[io.bloomventures/omni "0.32.2"]
            [lein-tools-deps "0.4.5"]]

  :omni-config dojo.omni-config/omni-config-prod

  :middleware [lein-tools-deps.plugin/resolve-dependencies-with-deps-edn]

  :lein-tools-deps/config {:config-files [:install :user :project]
                           :clojure-executables ["/opt/homebrew/bin/clojure"]}


  :main dojo.core
  :repl-options {:init-ns dojo.core
                 :timeout 200000}

  :jvm-opts ["-Dclojure.tools.logging.factory=clojure.tools.logging.impl/jul-factory"]

  :profiles {:uberjar
             {:aot :all
              :prep-tasks [["omni" "compile"]
                           "compile"]}})

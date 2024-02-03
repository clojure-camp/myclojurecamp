(defproject mycc "0.1.0-SNAPSHOT"

  :plugins [[io.bloomventures/omni "0.32.2"]
            [lein-tools-deps "0.4.5"]]

  :omni-config modulo.system/prod-omni-config

  :middleware [lein-tools-deps.plugin/resolve-dependencies-with-deps-edn]

  :lein-tools-deps/config {:config-files [:install :user :project]}

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

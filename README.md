# My Clojure Camp

App for managing pairing scheduler and other services of clojure camp.

## Developing

### Getting Started

(prerequisites: java, [leiningen](https://leiningen.org/), and [clojure cli](https://clojure.org/guides/getting_started#_clojure_installer_and_cli_tools))

`git clone`

`lein repl`

connect to the repl from your text editor

in `mycc.dev` (`dev-src/mycc/dev.clj`), run `(start!)` and `(seed/seed!)`


### Modules

Project is organized into feature-based modules:

- base - underlying "frameworky" glue code, shouldn't be called by any other modules
- common - utility namespaces used by other modules
- p2p - p2p scheduler
- fotd - fotd

Also of note are:
- mycc.api, which exposes certain framework fns to enable feature-based modules

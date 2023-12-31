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

- p2p - p2p scheduler
- profile - basic profile stuff
- fotd - function of the day

And supporting parts:

- modulo - our homebrew "frameworky" glue code, should only be accessed via `modulo.api`
- base - set up of non-module stuff, like header, auth - shouldn't be called by any other modules
- common - utility namespaces for use by modules

# Dojo

App for managing the Dojo.

WIP.

  - [ ] web UI to register and set topics
  - [x] on Friday evenings, job that sends email asking for opt-ins and updating schedule
  - [ ] web UI to update opt-in and schedule
  - [ ] web backend to store state
  - [ ] on Sunday evenings, job that calculates schedule and emails participants with schedule

## Developing

(prerequisites: java, [leiningen](https://leiningen.org/), and [clojure cli](https://clojure.org/guides/getting_started#_clojure_installer_and_cli_tools))

`git clone`

`lein repl`

connect to the repl from your text editor

in `dojo.core`, run `(config/generate!)` to generate a starter config

then, in `dojo.core`, run `(start!)` and `(seed/seed!)`

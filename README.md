# Dojo

App for managing the Dojo.

WIP.

  - [ ] web UI to register and set topics
  - [x] on Friday evenings, job that sends email asking for opt-ins and updating schedule
  - [ ] web UI to update opt-in and schedule
  - [ ] web backend to store state
  - [ ] on Sunday evenings, job that calculates schedule and emails participants with schedule

## Developing

(prerequisites: java, and leiningen)

`git clone`

`lein repl`

run `(dojo.config/generate "config.edn" dojo.config/schema)` to generate a starter config (change the values)

after you've filled in the `config.edn` either, restart the repl (`lein repl`) or `(require 'dojo.core :reload-all)`

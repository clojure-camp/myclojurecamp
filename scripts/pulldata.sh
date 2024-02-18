#!/bin/bash
current_dir=$(dirname "$0")
rsync -av --progress --delete clojurecamp:/www/myclojurecamp/data/data/ "$current_dir"/../external/

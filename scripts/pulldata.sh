#!/bin/bash
current_dir=$(dirname "$0")
rsync -run -av --delete clojurecamp:/www/myclojurecamp/data/data "$current_dir"/../external

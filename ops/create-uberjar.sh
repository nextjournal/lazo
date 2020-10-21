#!/bin/bash -e
mkdir -p classes
clj -e "(compile 'lazo.core)"
cd "$( dirname "${BASH_SOURCE[0]}" )"
clojure -Sdeps '{:deps {uberdeps/uberdeps {:mvn/version "1.0.2"}}}' \
        -m uberdeps.uberjar \
        --deps-file ../deps.edn \
        --target ../target/lazo.jar \
        --main-class lazo.core

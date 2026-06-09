#!/usr/bin/env bash
source ~/.sdkman/bin/sdkman-init.sh && ./gradlew quarkusDev --quarkus-args='greet -n Quarkus'

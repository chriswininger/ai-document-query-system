#! /usr/bin/env bash

description="${1:-new-migration}"
timestamp=$(date +%s)

touch "./src/main/resources/db/migration/V${timestamp}__${description}.sql"

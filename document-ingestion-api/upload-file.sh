#! /usr/bin/env bash

# supply a path to the the file 
curl -X POST http://localhost:8080/rest/v1/submit-document \
    -H "Content-Type: text/plain" \
    --data-binary @"$1"

#!/usr/bin/env bash

id=$1

curl -X POST \
  'http://localhost:8080/api/v1/rag/document/process-and-store' \
  -H 'Content-Type: application/json' \
  -d "{\"id\": $id}"


#! /usr/bin/env bash

data_dir="$(pwd)/db-data"
echo "data_dir: $data_dir"

docker run --name spring-ai-demo-db \
  --rm \
  -p 5436:5432 \
  -v "$data_dir/db":"/var/lib/postgresql/data" \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=xxx \
  -e POSTGRES_DB=spring-ai-demo-db \
  -d pgvector/pgvector:pg17

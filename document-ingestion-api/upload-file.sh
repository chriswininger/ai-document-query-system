#! /usr/bin/env bash
set -e

# supply a path to the file
TMPFILE=$(mktemp /tmp/upload-payload.XXXXXX.json)

echo "Building JSON payload from: $1"
cat "$1" | jq -Rs --arg pattern "CHAPTER .*\n\n" \
    '{document: ., chapterSplitPattern: $pattern}' > "$TMPFILE"

echo "Payload written to $TMPFILE ($(wc -c < "$TMPFILE") bytes), sending..."

curl -v -X POST http://localhost:8080/rest/v1/submit-document \
    -H "Content-Type: application/json" \
    --data @"$TMPFILE"

rm -f "$TMPFILE"

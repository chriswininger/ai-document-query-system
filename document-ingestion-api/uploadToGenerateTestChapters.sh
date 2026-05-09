#! /usr/bin/env bash
set -e

# supply a path to the file
TMPFILE=$(mktemp /tmp/upload-payload.XXXXXX.json)

TITLE=$(basename "$1" | sed 's/\.[^.]*$//')

echo "Building JSON payload from: $1 (title: $TITLE)"
cat "$1" | jq -Rs --arg pattern "CHAPTER .*\n\n" --arg title "$TITLE" \
    '{document: ., documentTitle: $title, chapterSplitPattern: $pattern}' > "$TMPFILE"

echo "Payload written to $TMPFILE ($(wc -c < "$TMPFILE") bytes), sending..."

curl -v -X POST http://localhost:8080/rest/v1/test/generate-test-chapters \
    -H "Content-Type: application/json" \
    --data @"$TMPFILE"

rm -f "$TMPFILE"

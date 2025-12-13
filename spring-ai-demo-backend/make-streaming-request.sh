#!/bin/bash

# Usage: ./make-streaming-request.sh "Your question here"
question=$1

if [ -z "$question" ]; then
    echo "Usage: ./make-streaming-request.sh \"Your question here\""
    exit 1
fi

# Construct JSON payload with the question, properly escaping JSON
json_payload=$(printf '{"userPrompt": "%s", "documentSourceIds": [10], "numberOfRagDocumentsToInclude": 5}' "$(echo "$question" | sed 's/"/\\"/g')")

curl -X POST \
	-H "Content-Type: application/json" \
	-H "Accept: text/event-stream" \
	-d "$json_payload" \
	http://localhost:8080/api/v1/chat/generic/stream


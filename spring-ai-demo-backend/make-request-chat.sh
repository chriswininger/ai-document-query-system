# example { "userPrompt": "Who are the flat earthers?" }
req=$1

curl -X POST \
	-H "Content-Type: application/json" \
	-H "Accept: application/json" \
	-d "$req" \
	http://localhost:8080/api/v1/chat

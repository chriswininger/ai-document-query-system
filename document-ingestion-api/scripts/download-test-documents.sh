#!/usr/bin/env bash
# Downloads testDocuments/ from the private test-data repo into
# src/test/resources/testDocuments/.
# Run from the workspace root:
#   bash document-ingestion-api/scripts/download-test-documents.sh
set -euo pipefail

REPO="git@github.com:chriswininger/document-ingestion-test-data.git"
DEST="$(cd "$(dirname "$0")/../src/test/resources" && pwd)/testDocuments"

TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT

echo "Cloning $REPO ..."
git clone --depth=1 "$REPO" "$TMP/repo"

if [ ! -d "$TMP/repo/testDocuments" ]; then
  echo "ERROR: testDocuments/ not found in remote repo — has it been uploaded yet?" >&2
  exit 1
fi

echo "Syncing to $DEST ..."
mkdir -p "$DEST"
# The archive deliberately omits .gitignore. Excluding it here keeps --delete
# from removing the local bare '*' that hides these fixtures from the public
# repo — it is untracked, so it could not be restored from git.
rsync -a --delete --exclude='.gitignore' "$TMP/repo/testDocuments/" "$DEST/"

echo "Done. Test documents are ready at $DEST"

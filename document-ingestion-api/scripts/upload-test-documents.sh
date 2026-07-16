#!/usr/bin/env bash
# Syncs src/test/resources/testDocuments/ to the private test-data repo.
# Run from the workspace root:
#   bash document-ingestion-api/scripts/upload-test-documents.sh
set -euo pipefail

REPO="git@github.com:chriswininger/document-ingestion-test-data.git"
SRC="$(cd "$(dirname "$0")/../src/test/resources/testDocuments" && pwd)"

if [ ! -d "$SRC" ]; then
  echo "ERROR: testDocuments directory not found at $SRC" >&2
  exit 1
fi

TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT

echo "Cloning $REPO ..."
git clone "$REPO" "$TMP/repo"

echo "Syncing testDocuments/ ..."
rsync -a --delete "$SRC/" "$TMP/repo/testDocuments/"

cd "$TMP/repo"

git add -A

if git diff --cached --quiet; then
  echo "No changes to push."
else
  git commit -m "Update test documents"
  git push
  echo "Done. Test documents pushed to $REPO"
fi

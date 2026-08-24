#!/usr/bin/env bash
# Syncs src/test/resources/testDocuments/ to the private test-data repo.
# Run from the workspace root:
#   bash document-ingestion-api/scripts/upload-test-documents.sh
#
# The sync mirrors deletions. If files would be removed from the archive the
# script stops and lists them; re-run with FORCE=1 to go ahead:
#   FORCE=1 bash document-ingestion-api/scripts/upload-test-documents.sh
set -euo pipefail

REPO="git@github.com:chriswininger/document-ingestion-test-data.git"
SRC="$(cd "$(dirname "$0")/../src/test/resources/testDocuments" && pwd)"

if [ ! -d "$SRC" ]; then
  echo "ERROR: testDocuments directory not found at $SRC" >&2
  exit 1
fi

# testDocuments/ is fully gitignored, so a fresh clone of this repo starts out
# empty. Mirroring that emptiness would wipe the only private copy.
if [ -z "$(find "$SRC" -type f -not -name '.gitignore' -print -quit)" ]; then
  echo "ERROR: $SRC contains no files — refusing to sync." >&2
  echo "       Run download-test-documents.sh first." >&2
  exit 1
fi

TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT

echo "Cloning $REPO ..."
git clone "$REPO" "$TMP/repo"

# The local testDocuments/.gitignore is a bare '*' that keeps these fixtures out
# of the public repo. Copying it into the archive would make git ignore every
# file we just synced, so it stays behind.
RSYNC_OPTS=(-a --delete --exclude='.gitignore')

DELETIONS=$(rsync "${RSYNC_OPTS[@]}" --dry-run --itemize-changes \
  "$SRC/" "$TMP/repo/testDocuments/" | { grep '^\*deleting' || true; })

if [ -n "$DELETIONS" ] && [ "${FORCE:-0}" != "1" ]; then
  echo "WARNING: this sync would remove the following from the archive:" >&2
  printf '%s\n' "$DELETIONS" >&2
  echo >&2
  echo "Re-run with FORCE=1 if that is intended." >&2
  exit 1
fi

echo "Syncing testDocuments/ ..."
rsync "${RSYNC_OPTS[@]}" "$SRC/" "$TMP/repo/testDocuments/"

cd "$TMP/repo"

git add -A

if git diff --cached --quiet; then
  echo "No changes to push."
else
  git commit -m "Update test documents"
  git push
  echo "Done. Test documents pushed to $REPO"
fi

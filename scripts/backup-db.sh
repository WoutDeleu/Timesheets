#!/usr/bin/env bash
# Daily cron: commit and push the H2 database to keep remote up to date.
set -e

REPO_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$REPO_DIR"

# Nothing to do if the DB hasn't changed
if git diff --quiet data/timesheets.mv.db; then
    echo "$(date): No DB changes, skipping." >> "$REPO_DIR/scripts/backup-db.log"
    exit 0
fi

git add data/timesheets.mv.db
git commit -m "chore: daily DB backup $(date '+%Y-%m-%d')"
git push

echo "$(date): DB backup pushed successfully." >> "$REPO_DIR/scripts/backup-db.log"

#!/usr/bin/env bash
set -euo pipefail

# self-test-report-2026-03-26-r2.md source-of-truth item #1
# Daily full backup with 30-day retention.

required=(DB_HOST DB_PORT DB_USER DB_PASSWORD DB_NAME BACKUP_DIR)
for v in "${required[@]}"; do
  if [ -z "${!v:-}" ]; then
    echo "Missing required env var: $v" >&2
    exit 1
  fi
done

timestamp="$(date +%Y%m%d_%H%M%S)"
mkdir -p "${BACKUP_DIR}/full"

mysqldump \
  --host="${DB_HOST}" \
  --port="${DB_PORT}" \
  --user="${DB_USER}" \
  --password="${DB_PASSWORD}" \
  --single-transaction \
  --routines --triggers --events \
  "${DB_NAME}" | gzip > "${BACKUP_DIR}/full/${DB_NAME}_full_${timestamp}.sql.gz"

find "${BACKUP_DIR}/full" -type f -name "*.sql.gz" -mtime +30 -delete
echo "Full backup completed: ${BACKUP_DIR}/full/${DB_NAME}_full_${timestamp}.sql.gz"

#!/usr/bin/env bash
set -euo pipefail

# self-test-report-2026-03-26-r2.md source-of-truth item #1
# Hourly incremental backup using binlog, with 30-day retention.

required=(DB_HOST DB_PORT DB_USER DB_PASSWORD DB_NAME BACKUP_DIR)
for v in "${required[@]}"; do
  if [ -z "${!v:-}" ]; then
    echo "Missing required env var: $v" >&2
    exit 1
  fi
done

timestamp="$(date +%Y%m%d_%H%M%S)"
mkdir -p "${BACKUP_DIR}/incremental"

mysql -h "${DB_HOST}" -P "${DB_PORT}" -u "${DB_USER}" -p"${DB_PASSWORD}" -Nse "SHOW BINARY LOGS" \
  | awk '{print $1}' \
  | while read -r logname; do
      mysqlbinlog \
        --read-from-remote-server \
        --host="${DB_HOST}" \
        --port="${DB_PORT}" \
        --user="${DB_USER}" \
        --password="${DB_PASSWORD}" \
        "${logname}" > "${BACKUP_DIR}/incremental/${timestamp}_${logname}.sql"
    done

find "${BACKUP_DIR}/incremental" -type f -name "*.sql" -mtime +30 -delete
echo "Incremental backup completed under ${BACKUP_DIR}/incremental"

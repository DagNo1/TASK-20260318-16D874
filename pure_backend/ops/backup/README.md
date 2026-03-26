# Backup and Recovery Runbook

Implemented per `self-test-report-2026-03-26-r2.md` (lines 194-198), item #1.

## Scope
- Daily full backup
- Hourly incremental backup
- Retention: 30 days

## Scripts
- `ops/backup/backup_full_daily.sh`
- `ops/backup/backup_incremental_hourly.sh`

## Required environment variables
- `DB_HOST`
- `DB_PORT`
- `DB_USER`
- `DB_PASSWORD`
- `DB_NAME`
- `BACKUP_DIR`

## Example schedule (cron)

```cron
0 2 * * * /workspace/ops/backup/backup_full_daily.sh
0 * * * * /workspace/ops/backup/backup_incremental_hourly.sh
```

## Restore steps

1. Stop application writers.
2. Restore latest full backup:

```bash
gzip -dc "$BACKUP_DIR/full/${DB_NAME}_full_YYYYMMDD_HHMMSS.sql.gz" \
  | mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASSWORD" "$DB_NAME"
```

3. Replay incremental backups in order:

```bash
for f in "$BACKUP_DIR"/incremental/*.sql; do
  mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" < "$f"
done
```

## Restore verification

1. Validate critical tables exist (`users`, `products`, `im_messages`, `notification_events`).
2. Verify row counts for known business tables are non-zero when expected.
3. Verify latest timestamps (e.g. max(`created_at`)) match expected recovery point.
4. Start app and run smoke checks for login, product query, and IM listing.

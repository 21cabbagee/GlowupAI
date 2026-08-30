# Database Setup and Best Practices Guide

This guide covers database configuration, migrations, performance tuning, and best practices for production.

## Overview

The GlowUp AI backend supports:
- **SQLite**: Development and testing (default)
- **PostgreSQL**: Production (recommended)

Database schema is automatically created on first startup using migrations in `skinproof/migrations/`.

## Quick Start

### Development (SQLite)

No configuration needed - just run the app:

```bash
# Uses .data/skinproof.sqlite3 by default
uvicorn skinproof.api:app
```

### Production (PostgreSQL)

1. **Create Database**:
   ```bash
   createdb skinproof_prod
   ```

2. **Configure Environment**:
   ```bash
   export DATABASE_URL="postgresql://user:password@localhost:5432/skinproof_prod"
   ```

3. **Run Application**:
   ```bash
   uvicorn skinproof.api:app
   ```

Schema is created automatically on first run.

## Connection Configuration

### Environment Variables

```bash
# Primary database URL (supports multiple formats)
DATABASE_URL=postgresql://user:password@host:5432/dbname
# or
POSTGRES_URL=postgresql://user:password@host:5432/dbname
# or
SKINPROOF_DATABASE_URL=postgresql://user:password@host:5432/dbname

# Connection pool settings
SKINPROOF_DB_POOL_MIN_SIZE=2           # Minimum connections in pool
SKINPROOF_DB_POOL_MAX_SIZE=20          # Maximum connections in pool
SKINPROOF_DB_CONNECT_TIMEOUT=10        # Connection timeout (seconds)
SKINPROOF_DB_POOL_TIMEOUT=30           # Pool acquisition timeout (seconds)
SKINPROOF_DB_STATEMENT_TIMEOUT=30000   # Query timeout (milliseconds)
```

### Connection Pool Sizing

**Formula**: `max_size = 2 * num_workers + spare`

**Examples**:
- 2 workers: `max_size = 6` (2*2 + 2 spare)
- 4 workers: `max_size = 10` (2*4 + 2 spare)
- 8 workers: `max_size = 20` (2*8 + 4 spare)

**Monitoring**:
```sql
-- Check current connections
SELECT count(*) as active_connections
FROM pg_stat_activity
WHERE datname = 'skinproof_prod';

-- Check connection pool stats (if using pgBouncer)
SHOW POOLS;
```

**Tuning**:
- Start conservative (max_size = 10)
- Monitor pool usage under load
- Increase if seeing "connection pool exhausted" errors
- Don't exceed database's max_connections limit

## Database Schema

### Core Tables

**users**
- Primary user records
- Indexed: `firebase_uid` (unique)
- Soft deletes via `deleted_at`

**products**
- Skincare products catalog
- Indexed: `barcode` (unique)
- Contains ingredient data

**captures**
- Photo captures with measurements
- Indexed: `user_id`, `captured_at`
- Large table - partition if needed

**routine_events**
- Product usage tracking
- Indexed: `user_id`, `timestamp`
- High write volume

**experiments**
- User experiments
- Indexed: `user_id`, `status`

### Critical Indexes

Current indexes (automatically created):
```sql
-- Users
CREATE INDEX idx_users_firebase_uid ON users(firebase_uid);

-- Captures (add if not present)
CREATE INDEX idx_captures_user_id ON captures(user_id);
CREATE INDEX idx_captures_captured_at ON captures(captured_at);

-- Routine Events (add if not present)
CREATE INDEX idx_routine_events_user_id ON routine_events(user_id);
CREATE INDEX idx_routine_events_timestamp ON routine_events(timestamp);

-- Experiments (add if not present)
CREATE INDEX idx_experiments_user_id ON experiments(user_id);
CREATE INDEX idx_experiments_status ON experiments(status);
```

### Verifying Indexes

```sql
-- List all indexes
SELECT tablename, indexname, indexdef
FROM pg_indexes
WHERE schemaname = 'public'
ORDER BY tablename, indexname;

-- Check index usage
SELECT schemaname, tablename, indexname,
       idx_scan as index_scans,
       idx_tup_read as tuples_read,
       idx_tup_fetch as tuples_fetched
FROM pg_stat_user_indexes
ORDER BY idx_scan ASC;
```

## Database Migrations

### How Migrations Work

1. Migration files in `skinproof/migrations/` (SQL files)
2. Schema version tracked in `schema_migrations` table
3. Migrations run automatically on startup
4. Each migration runs exactly once

### Current Migrations

- `001_initial.sql`: Base schema
- `002_*.sql`: Additional features (if any)

### Creating New Migrations

1. **Create SQL file**:
   ```bash
   # Format: NNN_description.sql
   touch skinproof/migrations/003_add_notifications.sql
   ```

2. **Write migration**:
   ```sql
   -- 003_add_notifications.sql
   CREATE TABLE notifications (
       id TEXT PRIMARY KEY,
       user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
       message TEXT NOT NULL,
       read_at TEXT,
       created_at TEXT NOT NULL DEFAULT (datetime('now'))
   );

   CREATE INDEX idx_notifications_user_id ON notifications(user_id);
   ```

3. **Test migration**:
   ```bash
   # Test on fresh database
   rm .data/test.db
   SKINPROOF_DB_PATH=.data/test.db uvicorn skinproof.api:app
   ```

4. **Rollback planning**: Always plan how to rollback
   ```sql
   -- rollback_003.sql (keep separate)
   DROP TABLE notifications;
   ```

### Best Practices for Migrations

- **Backward compatible**: New migrations shouldn't break existing code
- **Small and focused**: One logical change per migration
- **Tested**: Test on staging before production
- **Documented**: Comment complex migrations
- **Reversible**: Keep rollback scripts ready
- **Safe operations**:
  - ✅ Adding nullable columns
  - ✅ Adding tables
  - ✅ Adding indexes (with CONCURRENTLY in Postgres)
  - ⚠️ Dropping columns (ensure code doesn't reference them first)
  - ⚠️ Renaming columns (requires code changes)
  - ❌ Changing column types (often requires data migration)

## Performance Tuning

### Query Optimization

**1. Identify Slow Queries**:
```sql
-- Enable slow query logging (PostgreSQL)
ALTER DATABASE skinproof_prod SET log_min_duration_statement = 1000; -- 1 second

-- View slow queries
SELECT 
    query,
    calls,
    total_time,
    mean_time,
    max_time
FROM pg_stat_statements
ORDER BY mean_time DESC
LIMIT 20;
```

**2. Analyze Query Plans**:
```sql
EXPLAIN ANALYZE
SELECT * FROM captures
WHERE user_id = 'user_123'
ORDER BY captured_at DESC
LIMIT 20;
```

**3. Add Missing Indexes**:
```sql
-- If you see "Seq Scan" in EXPLAIN output for frequently queried columns
CREATE INDEX idx_table_column ON table(column);

-- For Postgres, use CONCURRENTLY to avoid blocking writes
CREATE INDEX CONCURRENTLY idx_table_column ON table(column);
```

### Database Maintenance

**1. Vacuum (PostgreSQL)**:
```sql
-- Reclaim space and update statistics
VACUUM ANALYZE;

-- For large tables
VACUUM ANALYZE captures;
```

**2. Analyze Statistics**:
```sql
-- Update query planner statistics
ANALYZE;
```

**3. Reindex (if needed)**:
```sql
-- Rebuild indexes (takes exclusive lock)
REINDEX TABLE captures;

-- Or use CONCURRENTLY (PostgreSQL 12+)
REINDEX INDEX CONCURRENTLY idx_captures_user_id;
```

**Schedule** (PostgreSQL):
- Daily: VACUUM ANALYZE (autovacuum usually handles this)
- Weekly: Manual ANALYZE on large tables
- Monthly: Check for index bloat

### Monitoring Queries

**Connection stats**:
```sql
SELECT 
    state,
    count(*) as connections
FROM pg_stat_activity
WHERE datname = 'skinproof_prod'
GROUP BY state;
```

**Table sizes**:
```sql
SELECT 
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;
```

**Index usage**:
```sql
SELECT 
    schemaname,
    tablename,
    indexname,
    idx_scan,
    pg_size_pretty(pg_relation_size(indexrelid)) as index_size
FROM pg_stat_user_indexes
WHERE schemaname = 'public'
ORDER BY idx_scan ASC;
```

## Backup and Recovery

### Backup Strategy

**1. Automated Backups** (Recommended):

Most managed PostgreSQL services include automated backups:
- Railway: Automatic daily backups (7-day retention)
- Render: Automatic daily backups (7-day retention)
- AWS RDS: Automated backups with point-in-time recovery
- Google Cloud SQL: Automated backups with configurable retention

**2. Manual Backups**:

```bash
# Full database backup
pg_dump $DATABASE_URL > backup_$(date +%Y%m%d).sql

# Compressed backup
pg_dump $DATABASE_URL | gzip > backup_$(date +%Y%m%d).sql.gz

# Backup to S3
pg_dump $DATABASE_URL | gzip | aws s3 cp - s3://backups/backup_$(date +%Y%m%d).sql.gz

# Backup specific table
pg_dump $DATABASE_URL -t captures > captures_backup.sql
```

**3. Backup Schedule**:
- **Daily**: Full database backup (retain 7 days)
- **Weekly**: Full backup to cold storage (retain 4 weeks)
- **Monthly**: Full backup to archive (retain 1 year)
- **Before migrations**: Always backup before schema changes

### Restore Procedures

**1. Full Restore**:
```bash
# Stop application first
railway stop

# Drop and recreate database
dropdb skinproof_prod
createdb skinproof_prod

# Restore
psql $DATABASE_URL < backup_20260831.sql

# Restart application
railway restart
```

**2. Point-in-Time Recovery** (if supported):
```bash
# AWS RDS example
aws rds restore-db-instance-to-point-in-time \
    --source-db-instance-identifier skinproof-prod \
    --target-db-instance-identifier skinproof-prod-restored \
    --restore-time 2026-08-31T10:00:00Z
```

**3. Partial Restore** (specific table):
```bash
# Restore just one table
pg_restore -t captures backup.sql | psql $DATABASE_URL
```

### Testing Restores

**Monthly Drill**:
1. Take backup
2. Restore to separate test database
3. Verify data integrity
4. Test application against restored database
5. Document time taken
6. Clean up test database

## Data Retention

### Retention Policies

Configure via environment variables:

```bash
# Raw photo retention (days)
SKINPROOF_RAW_RETENTION_DAYS=730  # 2 years default
```

### Cleanup Tasks

**1. Delete Old Photos** (if stored locally):
```python
# Implement cleanup job
from datetime import datetime, timedelta
from pathlib import Path

retention_days = 730
cutoff = datetime.now() - timedelta(days=retention_days)

# Query old captures
old_captures = db.fetchall(
    "SELECT id, photo_path FROM captures WHERE captured_at < ?",
    (cutoff.isoformat(),)
)

# Delete photos
for capture in old_captures:
    if capture['photo_path']:
        Path(capture['photo_path']).unlink(missing_ok=True)
```

**2. Archive Old Data**:
```sql
-- Move old experiments to archive table
CREATE TABLE experiments_archive AS
SELECT * FROM experiments
WHERE status = 'completed'
  AND updated_at < now() - interval '1 year';

-- Delete archived experiments
DELETE FROM experiments
WHERE status = 'completed'
  AND updated_at < now() - interval '1 year';
```

## High Availability

### Read Replicas

For read-heavy workloads:

```python
# Example: Route reads to replica
PRIMARY_URL = os.getenv("DATABASE_URL")
REPLICA_URL = os.getenv("DATABASE_REPLICA_URL")

# Write to primary
write_db = connect(PRIMARY_URL)

# Read from replica
read_db = connect(REPLICA_URL)
```

### Connection Pooling (PgBouncer)

For many concurrent connections:

```bash
# pgbouncer.ini
[databases]
skinproof_prod = host=db.example.com port=5432 dbname=skinproof_prod

[pgbouncer]
pool_mode = transaction
max_client_conn = 1000
default_pool_size = 20
```

## Security

### Access Control

```sql
-- Create read-only user for analytics
CREATE USER skinproof_readonly WITH PASSWORD 'secure_password';
GRANT CONNECT ON DATABASE skinproof_prod TO skinproof_readonly;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO skinproof_readonly;
```

### Encryption

- **At Rest**: Enable at database level (AWS RDS, Cloud SQL support this)
- **In Transit**: Always use SSL for connections
  ```bash
  DATABASE_URL=postgresql://user:pass@host:5432/db?sslmode=require
  ```

### Secrets Management

Never commit database credentials:
- ✅ Use environment variables
- ✅ Use secret management services (AWS Secrets Manager, HashiCorp Vault)
- ✅ Rotate credentials regularly
- ❌ Don't commit `.env` files
- ❌ Don't hardcode credentials

## Troubleshooting

### Common Issues

**1. Connection Pool Exhausted**:
```
Error: Could not acquire connection from pool
```

**Solution**:
- Increase `SKINPROOF_DB_POOL_MAX_SIZE`
- Check for connection leaks
- Verify queries are being closed properly

**2. Slow Queries**:
```
Query took > 5 seconds
```

**Solution**:
- Add indexes on frequently queried columns
- Optimize query (use EXPLAIN ANALYZE)
- Consider pagination for large result sets

**3. Disk Full**:
```
Error: No space left on device
```

**Solution**:
- Delete old data
- Vacuum to reclaim space
- Increase disk size
- Implement data archival

**4. Too Many Connections**:
```
Error: FATAL: remaining connection slots are reserved
```

**Solution**:
- Increase database `max_connections` setting
- Use connection pooler (PgBouncer)
- Reduce `SKINPROOF_DB_POOL_MAX_SIZE`

## Migration from SQLite to PostgreSQL

To migrate production data from SQLite to PostgreSQL:

```bash
# 1. Export from SQLite
sqlite3 .data/skinproof.sqlite3 .dump > dump.sql

# 2. Convert to PostgreSQL format
# (Handle syntax differences: datetime('now') -> now(), etc.)

# 3. Import to PostgreSQL
psql $DATABASE_URL < converted_dump.sql

# 4. Verify data
psql $DATABASE_URL -c "SELECT count(*) FROM users;"
```

Or use a migration tool:
```bash
pip install pgloader
pgloader .data/skinproof.sqlite3 $DATABASE_URL
```

## Resources

- PostgreSQL Documentation: https://www.postgresql.org/docs/
- SQLite Documentation: https://www.sqlite.org/docs.html
- Connection Pool Best Practices: See `config.py`
- Schema Definitions: See `db.py` and `postgres_db.py`

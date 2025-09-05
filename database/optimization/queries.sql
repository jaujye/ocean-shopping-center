-- Ocean Shopping Center - Query Optimization Scripts
-- Performance analysis and optimization for high-load scenarios

-- ====================
-- QUERY PERFORMANCE ANALYSIS
-- ====================

-- Enable query execution time logging
-- Add to postgresql.conf: log_min_duration_statement = 100

-- Top slow queries (requires pg_stat_statements extension)
-- CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

SELECT 
    query,
    calls,
    total_time,
    mean_time,
    stddev_time,
    rows,
    100.0 * shared_blks_hit / nullif(shared_blks_hit + shared_blks_read, 0) AS hit_percent
FROM pg_stat_statements 
ORDER BY mean_time DESC 
LIMIT 20;

-- ====================
-- CONNECTION POOL OPTIMIZATION
-- ====================

-- Current connection statistics
SELECT 
    datname,
    pid,
    usename,
    application_name,
    client_addr,
    state,
    backend_start,
    query_start,
    state_change
FROM pg_stat_activity 
WHERE state != 'idle'
ORDER BY backend_start;

-- Connection pool monitoring query
SELECT 
    count(*) as total_connections,
    count(*) FILTER (WHERE state = 'active') as active,
    count(*) FILTER (WHERE state = 'idle') as idle,
    count(*) FILTER (WHERE state = 'idle in transaction') as idle_in_transaction,
    count(*) FILTER (WHERE state = 'idle in transaction (aborted)') as idle_in_transaction_aborted
FROM pg_stat_activity;

-- ====================
-- TABLE STATISTICS AND BLOAT
-- ====================

-- Table sizes and row counts
SELECT 
    schemaname,
    tablename,
    n_tup_ins as inserts,
    n_tup_upd as updates,
    n_tup_del as deletes,
    n_live_tup as live_rows,
    n_dead_tup as dead_rows,
    round(100 * n_dead_tup / NULLIF(n_live_tup + n_dead_tup, 0), 2) as dead_row_percent,
    last_vacuum,
    last_autovacuum,
    last_analyze,
    last_autoanalyze
FROM pg_stat_user_tables
ORDER BY dead_row_percent DESC NULLS LAST;

-- Table bloat analysis
WITH table_bloat AS (
  SELECT 
    schemaname, 
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as table_size,
    pg_size_pretty(pg_relation_size(schemaname||'.'||tablename)) as relation_size,
    n_dead_tup,
    n_live_tup
  FROM pg_stat_user_tables
  WHERE n_dead_tup > 0
)
SELECT *,
  round(100 * n_dead_tup::numeric / NULLIF(n_live_tup + n_dead_tup, 0), 2) as bloat_percent
FROM table_bloat
ORDER BY bloat_percent DESC;

-- ====================
-- INDEX PERFORMANCE ANALYSIS
-- ====================

-- Index usage statistics
SELECT 
    schemaname,
    tablename,
    indexname,
    idx_tup_read,
    idx_tup_fetch,
    idx_scan,
    round(100 * idx_tup_read::numeric / NULLIF(idx_tup_read + seq_tup_read, 0), 2) as index_hit_rate
FROM pg_stat_user_indexes i
JOIN pg_stat_user_tables t ON i.relid = t.relid
ORDER BY idx_scan DESC;

-- Unused indexes (candidates for removal)
SELECT 
    schemaname,
    tablename,
    indexname,
    pg_size_pretty(pg_relation_size(indexname::regclass)) as size,
    idx_scan,
    idx_tup_read
FROM pg_stat_user_indexes
WHERE idx_scan < 10
ORDER BY pg_relation_size(indexname::regclass) DESC;

-- ====================
-- VACUUM AND ANALYZE OPTIMIZATION
-- ====================

-- Tables that need immediate attention
SELECT 
    schemaname,
    tablename,
    n_dead_tup,
    n_live_tup,
    round(100 * n_dead_tup::numeric / NULLIF(n_live_tup + n_dead_tup, 0), 2) as dead_row_percent,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as table_size,
    last_autovacuum,
    last_autoanalyze,
    CASE 
        WHEN last_autovacuum IS NULL THEN 'NEVER'
        WHEN last_autovacuum < NOW() - INTERVAL '1 day' THEN 'OVERDUE'
        ELSE 'OK'
    END as vacuum_status
FROM pg_stat_user_tables
WHERE n_dead_tup > 1000 OR 
      (n_dead_tup > 0 AND round(100 * n_dead_tup::numeric / NULLIF(n_live_tup + n_dead_tup, 0), 2) > 10)
ORDER BY dead_row_percent DESC;

-- Manual vacuum recommendations
SELECT 
    'VACUUM ANALYZE ' || schemaname || '.' || tablename || ';' as recommended_command
FROM pg_stat_user_tables
WHERE (n_dead_tup > 10000 OR 
       (n_dead_tup > 0 AND round(100 * n_dead_tup::numeric / NULLIF(n_live_tup + n_dead_tup, 0), 2) > 20))
  AND (last_autovacuum IS NULL OR last_autovacuum < NOW() - INTERVAL '1 day');

-- ====================
-- CACHE HIT RATIOS
-- ====================

-- Database cache hit ratio (should be > 95%)
SELECT 
    'Database Cache Hit Ratio' as metric,
    round(100.0 * sum(blks_hit) / sum(blks_hit + blks_read), 2) as percentage
FROM pg_stat_database;

-- Table cache hit ratios
SELECT 
    schemaname,
    tablename,
    round(100.0 * heap_blks_hit / (heap_blks_hit + heap_blks_read), 2) as cache_hit_ratio,
    heap_blks_hit,
    heap_blks_read
FROM pg_statio_user_tables
WHERE heap_blks_read > 0
ORDER BY cache_hit_ratio;

-- Index cache hit ratios
SELECT 
    schemaname,
    indexname,
    round(100.0 * idx_blks_hit / (idx_blks_hit + idx_blks_read), 2) as cache_hit_ratio,
    idx_blks_hit,
    idx_blks_read
FROM pg_statio_user_indexes
WHERE idx_blks_read > 0
ORDER BY cache_hit_ratio;

-- ====================
-- LOCK MONITORING
-- ====================

-- Current locks
SELECT 
    pl.pid,
    pa.usename,
    pa.application_name,
    pl.mode,
    pl.locktype,
    pl.relation::regclass,
    pl.granted,
    pa.query
FROM pg_locks pl
LEFT JOIN pg_stat_activity pa ON pl.pid = pa.pid
WHERE pl.pid != pg_backend_pid()
ORDER BY pl.granted, pl.pid;

-- Lock waits
SELECT 
    waiting.pid as waiting_pid,
    waiting_query.query as waiting_query,
    blocking.pid as blocking_pid,
    blocking_query.query as blocking_query,
    waiting.mode as waiting_mode,
    blocking.mode as blocking_mode
FROM pg_locks waiting
JOIN pg_stat_activity waiting_query ON waiting.pid = waiting_query.pid
JOIN pg_locks blocking ON (
    waiting.relation = blocking.relation 
    OR waiting.transactionid = blocking.transactionid
)
JOIN pg_stat_activity blocking_query ON blocking.pid = blocking_query.pid
WHERE NOT waiting.granted 
  AND blocking.granted
  AND waiting.pid != blocking.pid;

-- ====================
-- QUERY OPTIMIZATION RECOMMENDATIONS
-- ====================

-- Tables with high sequential scan ratio (need indexes)
SELECT 
    schemaname,
    tablename,
    seq_scan,
    seq_tup_read,
    idx_scan,
    idx_tup_fetch,
    round(100.0 * seq_tup_read / (seq_tup_read + idx_tup_fetch), 2) as seq_scan_ratio
FROM pg_stat_user_tables
WHERE seq_scan > 0
ORDER BY seq_scan_ratio DESC;

-- Large table scans (expensive operations)
SELECT 
    schemaname,
    tablename,
    seq_scan,
    seq_tup_read,
    seq_tup_read / seq_scan as avg_seq_tup_read,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as table_size
FROM pg_stat_user_tables
WHERE seq_scan > 0 AND seq_tup_read / seq_scan > 10000
ORDER BY avg_seq_tup_read DESC;

-- ====================
-- MAINTENANCE PROCEDURES
-- ====================

-- Reindex candidates (high bloat indexes)
SELECT 
    schemaname || '.' || indexname as index_name,
    'REINDEX INDEX CONCURRENTLY ' || schemaname || '.' || indexname || ';' as reindex_command
FROM pg_stat_user_indexes 
WHERE idx_scan > 1000  -- Only reindex frequently used indexes
  AND pg_relation_size(indexname::regclass) > 100 * 1024 * 1024; -- Only large indexes (>100MB)

-- Update table statistics
SELECT 'ANALYZE ' || schemaname || '.' || tablename || ';' as analyze_command
FROM pg_stat_user_tables
WHERE last_autoanalyze IS NULL 
   OR last_autoanalyze < NOW() - INTERVAL '7 days'
ORDER BY schemaname, tablename;

-- ====================
-- CONFIGURATION RECOMMENDATIONS
-- ====================

-- Current PostgreSQL configuration check
SELECT 
    name,
    setting,
    unit,
    source,
    context
FROM pg_settings 
WHERE name IN (
    'shared_buffers',
    'effective_cache_size', 
    'work_mem',
    'maintenance_work_mem',
    'max_connections',
    'autovacuum',
    'checkpoint_completion_target',
    'wal_buffers',
    'random_page_cost',
    'seq_page_cost'
)
ORDER BY name;

-- Memory usage analysis
SELECT 
    pg_size_pretty(pg_database_size(current_database())) as database_size,
    pg_size_pretty(sum(pg_total_relation_size(oid))) as total_table_size,
    pg_size_pretty(sum(pg_indexes_size(oid))) as total_index_size
FROM pg_class 
WHERE relkind IN ('r', 'i');

-- ====================
-- PERFORMANCE MONITORING VIEWS
-- ====================

-- Create a view for ongoing performance monitoring
CREATE OR REPLACE VIEW performance_summary AS
SELECT 
    'Cache Hit Ratio' as metric,
    round(100.0 * sum(blks_hit) / sum(blks_hit + blks_read), 2)::text || '%' as value,
    'Should be > 95%' as recommendation
FROM pg_stat_database
UNION ALL
SELECT 
    'Total Connections',
    count(*)::text,
    'Monitor against max_connections setting'
FROM pg_stat_activity
UNION ALL
SELECT 
    'Active Queries',
    count(*) FILTER (WHERE state = 'active')::text,
    'High values may indicate blocking'
FROM pg_stat_activity
UNION ALL
SELECT 
    'Tables Needing Vacuum',
    count(*)::text,
    'Should be low'
FROM pg_stat_user_tables
WHERE n_dead_tup > 1000 AND round(100 * n_dead_tup::numeric / NULLIF(n_live_tup + n_dead_tup, 0), 2) > 10;

-- Usage: SELECT * FROM performance_summary;
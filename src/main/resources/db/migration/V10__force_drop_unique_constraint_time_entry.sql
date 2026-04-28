-- V10: Robustly remove unique constraint/index on time_entry(entry_date, project_id)
-- V8 used DROP CONSTRAINT IF EXISTS which may have silently failed on existing databases.
-- This migration uses H2's INFORMATION_SCHEMA to find and drop any remaining unique index.

-- Drop the constraint again (safe with IF EXISTS)
ALTER TABLE time_entry DROP CONSTRAINT IF EXISTS uq_time_entry_date_project;

-- Also drop the backing unique index if it still exists
DROP INDEX IF EXISTS uq_time_entry_date_project;

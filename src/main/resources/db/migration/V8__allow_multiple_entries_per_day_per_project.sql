-- V8: Allow multiple time entries per day per project
-- Previously a unique constraint prevented logging e.g. morning at home + afternoon at office for the same project/day.
ALTER TABLE time_entry DROP CONSTRAINT IF EXISTS uq_time_entry_date_project;

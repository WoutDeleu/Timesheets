-- Add break duration tracking to time entries
ALTER TABLE time_entry ADD COLUMN break_duration DECIMAL(4,2) NOT NULL DEFAULT 0;

-- Add default break duration setting per project
ALTER TABLE project ADD COLUMN default_break_duration DECIMAL(4,2);

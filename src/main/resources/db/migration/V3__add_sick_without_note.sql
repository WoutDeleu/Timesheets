-- Add doctors_note flag to leave_entry (defaults to true for existing entries)
ALTER TABLE leave_entry ADD COLUMN doctors_note BOOLEAN DEFAULT TRUE NOT NULL;

-- Configurable limit for sick days without a doctor's note (Belgian default: 3/year)
INSERT INTO app_setting (setting_key, setting_value) VALUES ('sick_days_without_note_per_year', '3');

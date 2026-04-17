-- Mark a project as "internal" (employer-side work like AXXES admin).
-- Internal project hours are saldo-neutral: they don't generate positive or negative balance,
-- but they do fill deficits in client project targets on the same day.
ALTER TABLE project ADD COLUMN internal_project BOOLEAN NOT NULL DEFAULT FALSE;

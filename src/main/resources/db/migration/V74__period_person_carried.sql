-- Closing or soft-closing a period also fixes each person's carried figure for it (what they carried into it from
-- earlier periods), beside their due-total; reopening removes both. Null: not fixed (closed before this was kept).
ALTER TABLE period_person_total ADD COLUMN carried DECIMAL(14, 2);
ALTER TABLE hist.period_person_total ADD COLUMN carried DECIMAL(14, 2);
ALTER TABLE asof.period_person_total ADD COLUMN carried DECIMAL(14, 2);

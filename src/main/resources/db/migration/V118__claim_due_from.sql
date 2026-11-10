-- The date a claim's due-by was fixed from, once its date has been moved off it (null: its own date): a holiday
-- re-fix counts the terms it was fixed with from this date, then lays them after the claim's date.
ALTER TABLE claim ADD COLUMN due_from DATE;

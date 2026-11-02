-- The last day (the sheet's today then) a reminder was written for a claim; null when never reminded. An OPEN or
-- PENDING claim can be reminded once per sheet-today.
ALTER TABLE claim ADD COLUMN reminded DATE;

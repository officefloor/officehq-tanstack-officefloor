-- The sheet's "today": the date every date-dependent decision and default date on the sheet uses,
-- never the real clock. A sheet starts with the real date when it is created.
ALTER TABLE sheet ADD COLUMN today DATE;
UPDATE sheet SET today = CAST(created_at AS DATE) WHERE today IS NULL;
ALTER TABLE sheet ALTER COLUMN today SET NOT NULL;

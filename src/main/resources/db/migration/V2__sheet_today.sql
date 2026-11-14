-- The sheet's own "today": every date decision and default date on the sheet uses it, never the
-- real clock. It starts as the real date the sheet is first loaded.
ALTER TABLE sheet ADD COLUMN today DATE;
UPDATE sheet SET today = CAST(created_at AS DATE) WHERE today IS NULL;
ALTER TABLE sheet ALTER COLUMN today SET NOT NULL;

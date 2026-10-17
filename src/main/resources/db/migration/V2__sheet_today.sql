-- Each sheet has its own "today": every date decision and default date in the app uses it, never
-- the real clock. It starts as the real date when the sheet is first loaded.
ALTER TABLE sheet ADD COLUMN today DATE;
UPDATE sheet SET today = CURRENT_DATE WHERE today IS NULL;
ALTER TABLE sheet ALTER COLUMN today SET NOT NULL;

-- The sheet's own "today": every date decision and default date in the sheet uses it, never the
-- real clock. A sheet starts on the real date it was made.
ALTER TABLE sheet ADD COLUMN today DATE DEFAULT CURRENT_DATE NOT NULL;

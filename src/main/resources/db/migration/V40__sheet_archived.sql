-- A steward archives the sheet: everything on it stays visible and every change to it is refused. Not undone.
ALTER TABLE sheet ADD COLUMN archived BOOLEAN DEFAULT FALSE NOT NULL;

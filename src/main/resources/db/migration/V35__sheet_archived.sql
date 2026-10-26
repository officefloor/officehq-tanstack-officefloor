-- An archived sheet keeps everything visible and refuses every change. Only a steward archives a sheet.
ALTER TABLE sheet ADD COLUMN archived BOOLEAN DEFAULT FALSE NOT NULL;

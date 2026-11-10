-- A sheet a steward has archived: everything on it stays visible, and every change is refused.
ALTER TABLE sheet ADD COLUMN archived BOOLEAN DEFAULT FALSE NOT NULL;

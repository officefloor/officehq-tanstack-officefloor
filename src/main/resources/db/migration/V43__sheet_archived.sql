-- An archived sheet: a steward has archived it. Everything on it stays visible and every change to it is
-- refused (only watching, which is the user's own, is left).
ALTER TABLE sheet ADD COLUMN archived BOOLEAN DEFAULT FALSE NOT NULL;

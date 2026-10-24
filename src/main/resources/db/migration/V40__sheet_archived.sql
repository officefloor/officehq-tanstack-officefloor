-- Whether the sheet is archived: a steward archives it, and from then on everything stays visible and every change
-- is refused.
ALTER TABLE sheet ADD COLUMN archived BOOLEAN DEFAULT FALSE NOT NULL;

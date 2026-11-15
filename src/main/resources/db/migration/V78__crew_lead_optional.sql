-- A crew keeps going when its lead leaves the sheet (transferred away): it is then without a lead.
ALTER TABLE crew ALTER COLUMN lead_id SET NULL;

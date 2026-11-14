-- A crew keeps its name and members when its lead is transferred to another sheet: it then has no lead (null).
ALTER TABLE crew ALTER COLUMN lead_id SET NULL;

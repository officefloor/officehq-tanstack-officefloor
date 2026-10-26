-- A crew's lead may be gone: transferring the lead to another sheet keeps the crew, with no lead (null) and its
-- members as they were.
ALTER TABLE crew ALTER COLUMN lead_id SET NULL;
ALTER TABLE hist.crew ALTER COLUMN lead_id SET NULL;
ALTER TABLE asof.crew ALTER COLUMN lead_id SET NULL;

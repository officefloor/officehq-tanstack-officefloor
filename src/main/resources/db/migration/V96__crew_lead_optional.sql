-- A crew's lead may be none: a lead moved to another sheet leaves the crew without a lead.
ALTER TABLE crew ALTER COLUMN lead_id SET NULL;

-- A crew whose lead has left the sheet (transferred away) keeps its members and has no lead.
ALTER TABLE crew ALTER COLUMN lead_id DROP NOT NULL;

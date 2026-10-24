-- A person's name is unique on the sheet, ignoring case and surrounding spaces; names are stored trimmed.
UPDATE person SET name = TRIM(name);
ALTER TABLE person ADD COLUMN name_key VARCHAR(500) GENERATED ALWAYS AS (LOWER(name));
CREATE UNIQUE INDEX person_sheet_name ON person (sheet_id, name_key);

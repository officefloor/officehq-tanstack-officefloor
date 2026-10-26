-- A person's name is unique on the sheet, compared without regard to case. Names are stored without
-- surrounding spaces; name_key is the lower-cased name the uniqueness is kept on.
UPDATE person SET name = TRIM(name);
ALTER TABLE person ADD COLUMN name_key VARCHAR(255) GENERATED ALWAYS AS (LOWER(name));
ALTER TABLE person ADD CONSTRAINT person_sheet_name UNIQUE (sheet_id, name_key);

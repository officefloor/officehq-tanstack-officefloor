-- A person's name is unique on its sheet, compared without regard to case (names are stored trimmed).
ALTER TABLE person ADD COLUMN name_key VARCHAR(500) GENERATED ALWAYS AS (LOWER(name));
CREATE UNIQUE INDEX person_sheet_name ON person (sheet_id, name_key);

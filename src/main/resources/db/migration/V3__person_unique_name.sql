-- A person's name is unique on its sheet, compared without case and ignoring surrounding spaces.
-- Names are stored trimmed; `name_key` is the form they are compared in.
UPDATE person SET name = TRIM(name);
ALTER TABLE person ADD COLUMN name_key VARCHAR(500) GENERATED ALWAYS AS (LOWER(TRIM(name)));
CREATE UNIQUE INDEX person_sheet_name ON person (sheet_id, name_key);

-- A person's name is unique on its sheet, ignoring case and surrounding spaces (names are stored
-- trimmed; the app refuses a duplicate with a message, this index is the backstop).
ALTER TABLE person ADD COLUMN name_key VARCHAR(200) GENERATED ALWAYS AS (LOWER(TRIM(name)));
CREATE UNIQUE INDEX person_sheet_name ON person (sheet_id, name_key);

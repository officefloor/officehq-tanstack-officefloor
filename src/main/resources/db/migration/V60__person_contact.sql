-- A person's contact: stored trimmed and lower-cased, unique on its sheet. Null when they have none.
ALTER TABLE person ADD COLUMN contact VARCHAR(500);
CREATE UNIQUE INDEX person_sheet_contact ON person (sheet_id, contact);

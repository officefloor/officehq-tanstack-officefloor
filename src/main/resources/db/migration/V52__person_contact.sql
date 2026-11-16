-- A person's contact: kept without surrounding spaces and lower-cased, null when none. A contact is held by at most
-- one person on the sheet.
ALTER TABLE person ADD COLUMN contact VARCHAR(500);
CREATE UNIQUE INDEX person_sheet_contact ON person (sheet_id, contact);

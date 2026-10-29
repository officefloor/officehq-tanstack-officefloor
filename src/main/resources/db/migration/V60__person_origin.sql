-- The sheet a person was transferred from (origin_sheet_id) and the name they had there (origin_name); both null for
-- a person who was not transferred in. A person transferred again keeps only the sheet they last came from. Kept in the
-- person's history too (see V39). The journal trigger is made again around the change, as in V41.
DROP TRIGGER person_journal;
ALTER TABLE person ADD COLUMN origin_sheet_id VARCHAR(255);
ALTER TABLE person ADD COLUMN origin_name VARCHAR(255);
ALTER TABLE person_history ADD COLUMN origin_sheet_id VARCHAR(255);
ALTER TABLE person_history ADD COLUMN origin_name VARCHAR(255);
CREATE TRIGGER person_journal AFTER INSERT, UPDATE, DELETE ON person FOR EACH ROW CALL 'net.officefloor.hq.app.history.Journal';

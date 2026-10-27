-- A person's contact, kept without surrounding spaces and lower-cased; null for none. No two people on a sheet share
-- one. Kept in the person's history too (see V39). The journal trigger is made again around the change, as in V41.
DROP TRIGGER person_journal;
ALTER TABLE person ADD COLUMN contact VARCHAR(255);
ALTER TABLE person_history ADD COLUMN contact VARCHAR(255);
CREATE TRIGGER person_journal AFTER INSERT, UPDATE, DELETE ON person FOR EACH ROW CALL 'net.officefloor.hq.app.history.Journal';

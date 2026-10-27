-- The order a person named their patrons in (a patron's share depends on it: the first named takes what is left
-- over): each link numbered from one sequence as it is made. Kept in the patrons' history too (see V40). The journal
-- trigger is made again around the change, as in V41.
DROP TRIGGER person_patron_journal;
CREATE SEQUENCE person_patron_named;
ALTER TABLE person_patron ADD COLUMN named BIGINT DEFAULT NEXT VALUE FOR person_patron_named NOT NULL;
ALTER TABLE person_patron_history ADD COLUMN named BIGINT;
CREATE TRIGGER person_patron_journal AFTER INSERT, UPDATE, DELETE ON person_patron FOR EACH ROW CALL 'net.officefloor.hq.app.history.Journal';

-- A person's position in the sheet's list: the list (and everything worked out in list order) runs by position, lowest
-- first. A new person (added, imported or transferred in) goes after everyone on the sheet; moving a person up or down
-- swaps positions with their neighbour. Positions may have gaps (a person merged away or transferred out leaves one).
-- People stored before keep the order they were added in. Kept in the person's history too (see V39). The journal
-- trigger is made again around the change, as in V41.
DROP TRIGGER person_journal;
ALTER TABLE person ADD COLUMN list_position BIGINT;
UPDATE person SET list_position = id;
ALTER TABLE person ALTER COLUMN list_position SET NOT NULL;
ALTER TABLE person_history ADD COLUMN list_position BIGINT;
UPDATE person_history SET list_position = id;
CREATE TRIGGER person_journal AFTER INSERT, UPDATE, DELETE ON person FOR EACH ROW CALL 'net.officefloor.hq.app.history.Journal';

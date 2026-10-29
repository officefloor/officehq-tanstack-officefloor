-- The sheet's today when a person arrived by transfer (transferred_on); null for a person who was not transferred in.
-- A transfer may be undone on the sheet it brought them to only on that same sheet-today. Kept in the person's
-- history too (see V39). The journal trigger is made again around the change, as in V41.
DROP TRIGGER person_journal;
ALTER TABLE person ADD COLUMN transferred_on DATE;
ALTER TABLE person_history ADD COLUMN transferred_on DATE;
CREATE TRIGGER person_journal AFTER INSERT, UPDATE, DELETE ON person FOR EACH ROW CALL 'net.officefloor.hq.app.history.Journal';

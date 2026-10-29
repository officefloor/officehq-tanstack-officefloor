-- A person may carry tags, kept as a claim's are (see V45): trimmed, lower-cased, without repeats and sorted, comma
-- separated (null: none). Kept in the person's history too (see V39), where the tag totals read every tag ever used on
-- a sheet from. The journal trigger is made again around the change, as in V41.
DROP TRIGGER person_journal;
ALTER TABLE person ADD COLUMN tags VARCHAR(4000);
ALTER TABLE person_history ADD COLUMN tags VARCHAR(4000);
CREATE TRIGGER person_journal AFTER INSERT, UPDATE, DELETE ON person FOR EACH ROW CALL 'net.officefloor.hq.app.history.Journal';

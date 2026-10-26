-- A claim may carry tags: kept trimmed, lower-cased, without repeats and sorted, comma separated (null: none). Kept in
-- the claim's history too (see V39), which is where every tag ever used on a sheet is read from. The journal trigger is
-- made again around the change, as in V41.
DROP TRIGGER claim_journal;
ALTER TABLE claim ADD COLUMN tags VARCHAR(4000);
ALTER TABLE claim_history ADD COLUMN tags VARCHAR(4000);
CREATE TRIGGER claim_journal AFTER INSERT, UPDATE, DELETE ON claim FOR EACH ROW CALL 'net.officefloor.hq.app.history.Journal';

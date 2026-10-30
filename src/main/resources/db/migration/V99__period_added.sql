-- The order a sheet's periods were added in (a rising number; the periods are otherwise kept by from-date), so the
-- period figures can be exported period by period as they were added. Kept in the period's history too (see V40).
-- The journal trigger is made again around the change, as in V70.
DROP TRIGGER period_journal;
CREATE SEQUENCE period_added_seq;
ALTER TABLE period ADD COLUMN added BIGINT DEFAULT NEXT VALUE FOR period_added_seq NOT NULL;
ALTER TABLE period_history ADD COLUMN added BIGINT;
CREATE TRIGGER period_journal AFTER INSERT, UPDATE, DELETE ON period FOR EACH ROW CALL 'net.officefloor.hq.app.history.Journal';

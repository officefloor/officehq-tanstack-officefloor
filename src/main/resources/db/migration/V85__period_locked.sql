-- A steward may lock a period, and unlock it again: a locked period cannot be reopened. Kept in the period's history
-- too (see V40). The journal trigger is made again around the change, as in V70.
DROP TRIGGER period_journal;
ALTER TABLE period ADD COLUMN locked BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE period_history ADD COLUMN locked BOOLEAN;
CREATE TRIGGER period_journal AFTER INSERT, UPDATE, DELETE ON period FOR EACH ROW CALL 'net.officefloor.hq.app.history.Journal';

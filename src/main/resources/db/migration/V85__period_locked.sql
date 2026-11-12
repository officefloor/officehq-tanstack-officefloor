-- A steward may lock a period: a locked period cannot be reopened until it is unlocked. Kept in period_hist
-- too; the history trigger is set again afterwards, as in V69__period_cap.sql.
DROP TRIGGER period_history;
ALTER TABLE period ADD COLUMN locked BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE period_hist ADD COLUMN locked BOOLEAN DEFAULT FALSE NOT NULL;
CREATE TRIGGER period_history AFTER INSERT, UPDATE, DELETE ON period FOR EACH ROW
    CALL 'net.officefloor.hq.app.history.HistoryTrigger';

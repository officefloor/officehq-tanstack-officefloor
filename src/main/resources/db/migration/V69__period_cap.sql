-- A period may cap the number of open claims per person: a claim belonging to it (its due-by falls in it) may
-- not be opened while its person already has cap OPEN or PENDING claims in it. Null for no cap. Kept in
-- period_hist too; the history trigger is set again afterwards, as in V60__pool_member_dates.sql.
DROP TRIGGER period_history;
ALTER TABLE period ADD COLUMN open_cap INT;
ALTER TABLE period_hist ADD COLUMN open_cap INT;
CREATE TRIGGER period_history AFTER INSERT, UPDATE, DELETE ON period FOR EACH ROW
    CALL 'net.officefloor.hq.app.history.HistoryTrigger';

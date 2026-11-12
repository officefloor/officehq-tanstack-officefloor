-- A pool membership may carry a from-date and a to-date (either null for no bound; a to-date before the
-- from-date is refused, see ValidateMember). A member whose dates do not include the sheet's today is left
-- out of the pool's total share, its balance and its cuts. Kept in pool_member_hist too; the history trigger
-- is set again afterwards, as in V45__claim_tags.sql.
DROP TRIGGER pool_member_history;
ALTER TABLE pool_member ADD COLUMN from_date DATE;
ALTER TABLE pool_member ADD COLUMN to_date DATE;
ALTER TABLE pool_member_hist ADD COLUMN from_date DATE;
ALTER TABLE pool_member_hist ADD COLUMN to_date DATE;
CREATE TRIGGER pool_member_history AFTER INSERT, UPDATE, DELETE ON pool_member FOR EACH ROW
    CALL 'net.officefloor.hq.app.history.HistoryTrigger';

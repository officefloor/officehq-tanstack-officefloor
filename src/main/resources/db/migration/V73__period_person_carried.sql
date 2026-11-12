-- Each person's carried figure for a closed or soft-closed period, fixed with their due-total when the period
-- was closed (the figures of their claims due in an earlier period and still OPEN or PENDING at its end,
-- skipping n/a). Dropped with it on reopening. Kept in period_person_total_hist too (see V40__as_of_view.sql);
-- the history trigger is set again afterwards, as in V45__claim_tags.sql.
DROP TRIGGER period_person_total_history;
ALTER TABLE period_person_total ADD COLUMN carried DECIMAL(19, 2) DEFAULT 0 NOT NULL;
ALTER TABLE period_person_total_hist ADD COLUMN carried DECIMAL(19, 2) DEFAULT 0 NOT NULL;
CREATE TRIGGER period_person_total_history AFTER INSERT, UPDATE, DELETE ON period_person_total FOR EACH ROW
    CALL 'net.officefloor.hq.app.history.HistoryTrigger';

-- Closing or soft-closing a period fixes each person's carried figure for it too (the due of their earlier periods'
-- claims still OPEN or PENDING at its end), beside their due-total; reopening drops it with the row. Null for a row
-- fixed before this. Kept in history too (see V40); the journal trigger is made again around the change, as in V70.
DROP TRIGGER period_person_total_journal;
ALTER TABLE period_person_total ADD COLUMN carried DECIMAL(15, 2);
ALTER TABLE period_person_total_history ADD COLUMN carried DECIMAL(15, 2);
CREATE TRIGGER period_person_total_journal AFTER INSERT, UPDATE, DELETE ON period_person_total FOR EACH ROW CALL 'net.officefloor.hq.app.history.Journal';

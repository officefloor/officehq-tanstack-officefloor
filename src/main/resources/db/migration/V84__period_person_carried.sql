-- Each person's carried figure for a closed (or soft-closed) period, fixed when it was closed with
-- their due-total for it: the due of their counted claims belonging, by due-by, to an earlier period
-- that were still OPEN or PENDING at the end of its last day. Reopening deletes the rows.
DROP TRIGGER public.period_person_total_history;
ALTER TABLE public.period_person_total ADD COLUMN carried DECIMAL(14,2) DEFAULT 0 NOT NULL;
ALTER TABLE asof.period_person_total ADD COLUMN carried DECIMAL(14,2) DEFAULT 0 NOT NULL;
ALTER TABLE asof_hist.period_person_total ADD COLUMN carried DECIMAL(14,2) DEFAULT 0 NOT NULL;
-- The history trigger keeps the column list it was created with: create it again to see the new one.
CREATE TRIGGER period_person_total_history AFTER INSERT, UPDATE, DELETE ON public.period_person_total FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';

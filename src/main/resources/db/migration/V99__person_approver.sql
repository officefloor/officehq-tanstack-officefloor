-- A person may have an approver: a user who may approve or reject their PENDING claims as a steward
-- may. Null when none.
ALTER TABLE public.person ADD COLUMN approver VARCHAR(255);
ALTER TABLE asof.person ADD COLUMN approver VARCHAR(255);
ALTER TABLE asof_hist.person ADD COLUMN approver VARCHAR(255);
-- The history trigger keeps the column list it was created with: create it again to see the new one.
DROP TRIGGER public.person_history;
CREATE TRIGGER person_history AFTER INSERT, UPDATE, DELETE ON public.person FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';

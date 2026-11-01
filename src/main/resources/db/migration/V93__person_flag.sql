-- A steward can flag a person with a reason until a date (inclusive): while the sheet's today is on
-- or before it, only a steward may open a claim of theirs. Null when never flagged.
ALTER TABLE public.person ADD COLUMN flag_reason VARCHAR(255);
ALTER TABLE public.person ADD COLUMN flag_until DATE;
ALTER TABLE asof.person ADD COLUMN flag_reason VARCHAR(255);
ALTER TABLE asof.person ADD COLUMN flag_until DATE;
ALTER TABLE asof_hist.person ADD COLUMN flag_reason VARCHAR(255);
ALTER TABLE asof_hist.person ADD COLUMN flag_until DATE;
-- The history trigger keeps the column list it was created with: create it again to see the new one.
DROP TRIGGER public.person_history;
CREATE TRIGGER person_history AFTER INSERT, UPDATE, DELETE ON public.person FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';

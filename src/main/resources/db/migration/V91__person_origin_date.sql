-- The arriving sheet's today when a transferred person arrived (null: not transferred here). The
-- transfer can be undone on that sheet-today only.
ALTER TABLE public.person ADD COLUMN origin_date DATE;
ALTER TABLE asof.person ADD COLUMN origin_date DATE;
ALTER TABLE asof_hist.person ADD COLUMN origin_date DATE;
-- The history trigger keeps the column list it was created with: create it again to see the new one.
DROP TRIGGER public.person_history;
CREATE TRIGGER person_history AFTER INSERT, UPDATE, DELETE ON public.person FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';

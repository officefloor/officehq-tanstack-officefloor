-- A period's cap: the most OPEN or PENDING claims a person may have due by in it. Null for none.
ALTER TABLE public.period ADD COLUMN cap INTEGER;
ALTER TABLE asof.period ADD COLUMN cap INTEGER;
ALTER TABLE asof_hist.period ADD COLUMN cap INTEGER;
-- The history trigger keeps the column list it was created with: create it again to see the new one.
DROP TRIGGER public.period_history;
CREATE TRIGGER period_history AFTER INSERT, UPDATE, DELETE ON public.period FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';

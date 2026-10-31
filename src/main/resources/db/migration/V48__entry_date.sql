-- The sheet's entry date: while set, a new claim with no date given is dated it in place of the
-- sheet's today. Null when unset (off).
ALTER TABLE public.sheet_setting ADD COLUMN entry_date DATE;
ALTER TABLE asof.sheet_setting ADD COLUMN entry_date DATE;
ALTER TABLE asof_hist.sheet_setting ADD COLUMN entry_date DATE;
-- The history trigger keeps the column list it was created with: create it again to see the new one.
DROP TRIGGER public.sheet_setting_history;
CREATE TRIGGER sheet_setting_history AFTER INSERT, UPDATE, DELETE ON public.sheet_setting FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';

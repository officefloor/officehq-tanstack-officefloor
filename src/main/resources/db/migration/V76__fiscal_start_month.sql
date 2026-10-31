-- The sheet's fiscal year start month (1 to 12): fiscal year FY<yyyy> starts on its first day in
-- yyyy. Null when unset (1, January). Not dated.
ALTER TABLE public.sheet_setting ADD COLUMN fiscal_start_month INT;
ALTER TABLE asof.sheet_setting ADD COLUMN fiscal_start_month INT;
ALTER TABLE asof_hist.sheet_setting ADD COLUMN fiscal_start_month INT;
-- The history trigger keeps the column list it was created with: create it again to see the new one.
DROP TRIGGER public.sheet_setting_history;
CREATE TRIGGER sheet_setting_history AFTER INSERT, UPDATE, DELETE ON public.sheet_setting FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';

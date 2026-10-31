-- The sheet's parent sheet: a grade with no rate in force on a date on this sheet takes the
-- parent's rate then, else its parent's, up to three levels up. Null when unset (none). Not dated.
ALTER TABLE public.sheet_setting ADD COLUMN parent_id VARCHAR(200);
ALTER TABLE asof.sheet_setting ADD COLUMN parent_id VARCHAR(200);
ALTER TABLE asof_hist.sheet_setting ADD COLUMN parent_id VARCHAR(200);
-- The history trigger keeps the column list it was created with: create it again to see the new one.
DROP TRIGGER public.sheet_setting_history;
CREATE TRIGGER sheet_setting_history AFTER INSERT, UPDATE, DELETE ON public.sheet_setting FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';

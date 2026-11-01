-- The sheet's default grade: the grade a person added by the quick-add bar, or by an import line
-- with a blank grade, is given. One of A, B, C; null when unset (A). Not dated.
ALTER TABLE public.sheet_setting ADD COLUMN default_grade VARCHAR(1);
ALTER TABLE asof.sheet_setting ADD COLUMN default_grade VARCHAR(1);
ALTER TABLE asof_hist.sheet_setting ADD COLUMN default_grade VARCHAR(1);
DROP TRIGGER public.sheet_setting_history;
CREATE TRIGGER sheet_setting_history AFTER INSERT, UPDATE, DELETE ON public.sheet_setting FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';
